#!/usr/bin/env python3
"""Generate a Markdown summary from Kover XML reports."""

from __future__ import annotations

import argparse
import json
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET


COUNTER_TYPES = ("LINE", "BRANCH", "INSTRUCTION")


@dataclass(frozen=True)
class Counter:
    missed: int = 0
    covered: int = 0

    @property
    def total(self) -> int:
        return self.missed + self.covered

    @property
    def percent(self) -> float:
        if self.total == 0:
            return 0.0
        return self.covered * 100 / self.total

    @classmethod
    def from_element(cls, element: ET.Element | None) -> "Counter":
        if element is None:
            return cls()
        return cls(
            missed=int(element.attrib.get("missed", "0")),
            covered=int(element.attrib.get("covered", "0")),
        )

    def as_dict(self) -> dict[str, int | float]:
        return {
            "missed": self.missed,
            "covered": self.covered,
            "total": self.total,
            "percent": round(self.percent, 2),
        }


@dataclass(frozen=True)
class ClassCoverage:
    name: str
    source_file: str
    counters: dict[str, Counter]

    def counter(self, counter_type: str) -> Counter:
        return self.counters.get(counter_type, Counter())


@dataclass(frozen=True)
class CoverageReport:
    label: str
    xml_path: Path
    generated: bool
    counters: dict[str, Counter]
    classes: list[ClassCoverage]

    def counter(self, counter_type: str) -> Counter:
        return self.counters.get(counter_type, Counter())


def parse_report(label: str, xml_path: Path) -> CoverageReport:
    if not xml_path.exists():
        return CoverageReport(
            label=label,
            xml_path=xml_path,
            generated=False,
            counters={},
            classes=[],
        )

    root = ET.parse(xml_path).getroot()
    counters = parse_counters(root)
    classes = [
        ClassCoverage(
            name=class_element.attrib["name"].replace("/", "."),
            source_file=class_element.attrib.get("sourcefilename", ""),
            counters=parse_counters(class_element),
        )
        for class_element in root.findall(".//class")
    ]
    return CoverageReport(
        label=label,
        xml_path=xml_path,
        generated=True,
        counters=counters,
        classes=classes,
    )


def parse_counters(element: ET.Element) -> dict[str, Counter]:
    return {
        counter_type: Counter.from_element(element.find(f"counter[@type='{counter_type}']"))
        for counter_type in COUNTER_TYPES
    }


def read_baseline(path: Path | None) -> dict:
    if path is None or not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def baseline_percent(
    baseline: dict,
    report_label: str,
    counter_type: str,
) -> float | None:
    report = baseline.get("reports", {}).get(report_label)
    if not report:
        return None
    counter = report.get(counter_type)
    if not counter:
        return None
    if "percent" in counter:
        return float(counter["percent"])
    total = int(counter.get("total", 0))
    covered = int(counter.get("covered", 0))
    if total == 0:
        return 0.0
    return covered * 100 / total


def format_percent(counter: Counter) -> str:
    return f"{counter.percent:.2f}% ({counter.covered}/{counter.total})"


def format_delta(current: float, previous: float | None) -> str:
    if previous is None:
        return "n/a"
    delta = current - previous
    if abs(delta) < 0.005:
        delta = 0.0
    sign = "+" if delta >= 0 else ""
    return f"{sign}{delta:.2f}%p"


def low_coverage_classes(
    report: CoverageReport,
    threshold: float,
    limit: int,
) -> list[ClassCoverage]:
    candidates = [
        item
        for item in report.classes
        if item.counter("LINE").total > 0 and item.counter("LINE").percent < threshold
    ]
    return sorted(
        candidates,
        key=lambda item: (item.counter("LINE").percent, -item.counter("LINE").total, item.name),
    )[:limit]


def recommendation_for(item: ClassCoverage) -> str:
    lower_name = item.name.lower()
    line = item.counter("LINE").percent
    branch = item.counter("BRANCH").percent
    branch_total = item.counter("BRANCH").total

    if "viewmodel" in lower_name:
        return "상태 전이, 마지막 질문 처리, reset 동작을 ViewModel 단위 테스트로 보강"
    if "calculator" in lower_name:
        return "동점, 빈 답변, 잘못된 result id 같은 결과 계산 분기 테스트 보강"
    if any(keyword in lower_name for keyword in ("parser", "mapper", "repository")):
        return "정상/누락/잘못된 데이터 입력을 나눠 data layer 테스트 보강"
    if branch_total > 0 and branch < 80:
        return "조건 분기별 입력을 추가해 branch coverage 보강"
    if line < 80:
        return "주요 public 동작을 기준으로 line coverage 보강"
    return "현재 coverage 유지 여부 확인"


def build_markdown(
    reports: list[CoverageReport],
    baseline: dict,
    output_path: Path,
    low_threshold: float,
    low_limit: int,
) -> str:
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    lines = [
        "# Coverage Summary",
        "",
        f"- generated at: {generated_at}",
        f"- output: `{output_path.as_posix()}`",
        "- quality signal: focused debug coverage",
        "- full reference coverage is informational only",
        "",
        "## Current Coverage",
        "",
        "| Report | LINE | Delta | BRANCH | Delta | INSTRUCTION | Delta | XML |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
    ]

    for report in reports:
        if not report.generated:
            lines.append(
                f"| {report.label} | not generated | n/a | not generated | n/a | not generated | n/a | `{report.xml_path.as_posix()}` |"
            )
            continue

        row = [f"| {report.label}"]
        for counter_type in COUNTER_TYPES:
            counter = report.counter(counter_type)
            previous = baseline_percent(baseline, report.label, counter_type)
            row.append(format_percent(counter))
            row.append(format_delta(counter.percent, previous))
        row.append(f"`{report.xml_path.as_posix()}` |")
        lines.append(" | ".join(row))

    focused = next((report for report in reports if report.label == "focused debug"), reports[0])
    low_items = low_coverage_classes(focused, low_threshold, low_limit)

    lines.extend(
        [
            "",
            "## Low Coverage Areas",
            "",
        ]
    )
    if not focused.generated:
        lines.append("- focused report was not generated.")
    elif not low_items:
        lines.append(f"- No focused classes below {low_threshold:.0f}% line coverage.")
    else:
        lines.extend(
            [
                "| Class | Source | LINE | BRANCH | INSTRUCTION |",
                "| --- | --- | ---: | ---: | ---: |",
            ]
        )
        for item in low_items:
            lines.append(
                " | ".join(
                    [
                        f"| `{item.name}`",
                        f"`{item.source_file}`",
                        format_percent(item.counter("LINE")),
                        format_percent(item.counter("BRANCH")),
                        f"{format_percent(item.counter('INSTRUCTION'))} |",
                    ]
                )
            )

    lines.extend(
        [
            "",
            "## Next Test Candidates",
            "",
        ]
    )
    if not focused.generated:
        lines.append("- Generate focused Kover XML before selecting test candidates.")
    elif not low_items:
        lines.append("- Coverage is above the current low-coverage threshold. Consider enabling a coverage gate.")
    else:
        seen: set[str] = set()
        for item in low_items:
            recommendation = recommendation_for(item)
            if recommendation in seen:
                continue
            seen.add(recommendation)
            lines.append(f"- `{item.name}`: {recommendation}")

    lines.extend(
        [
            "",
            "## AI Analysis Notes",
            "",
            "- Treat focused debug coverage as the main production-code quality signal.",
            "- Use full reference coverage only to explain why Compose/UI glue is not part of the pass/fail number.",
            "- Before applying an 80% gate, prioritize ResultCalculator and ViewModel tests.",
            "",
        ]
    )
    return "\n".join(lines)


def build_json(reports: list[CoverageReport], low_threshold: float, low_limit: int) -> dict:
    focused = next((report for report in reports if report.label == "focused debug"), reports[0])
    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "qualitySignal": "focused debug",
        "reports": {
            report.label: {
                "xmlPath": report.xml_path.as_posix(),
                "generated": report.generated,
                **{counter_type: report.counter(counter_type).as_dict() for counter_type in COUNTER_TYPES},
            }
            for report in reports
        },
        "lowCoverageAreas": [
            {
                "className": item.name,
                "sourceFile": item.source_file,
                "line": item.counter("LINE").as_dict(),
                "branch": item.counter("BRANCH").as_dict(),
                "instruction": item.counter("INSTRUCTION").as_dict(),
                "recommendation": recommendation_for(item),
            }
            for item in low_coverage_classes(focused, low_threshold, low_limit)
        ]
        if focused.generated
        else [],
    }


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--focused-xml", type=Path, default=Path("app/build/reports/kover/reportDebug.xml"))
    parser.add_argument("--full-xml", type=Path, default=Path("app/build/reports/kover/report.xml"))
    parser.add_argument("--baseline", type=Path, default=Path("docs/coverage-baseline.json"))
    parser.add_argument("--output", type=Path, default=Path("build/reports/coverage-summary/coverage-summary.md"))
    parser.add_argument("--json-output", type=Path, default=Path("build/reports/coverage-summary/coverage-summary.json"))
    parser.add_argument("--append-step-summary", action="store_true")
    parser.add_argument("--low-threshold", type=float, default=80.0)
    parser.add_argument("--low-limit", type=int, default=10)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    reports = [
        parse_report("focused debug", args.focused_xml),
        parse_report("full reference", args.full_xml),
    ]
    baseline = read_baseline(args.baseline)

    markdown = build_markdown(
        reports=reports,
        baseline=baseline,
        output_path=args.output,
        low_threshold=args.low_threshold,
        low_limit=args.low_limit,
    )
    write_text(args.output, markdown)
    write_text(args.json_output, json.dumps(build_json(reports, args.low_threshold, args.low_limit), indent=2) + "\n")

    if args.append_step_summary:
        summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary_path:
            with Path(summary_path).open("a", encoding="utf-8") as summary:
                summary.write(markdown)
                summary.write("\n")

    print(f"Coverage summary written to {args.output}")
    print(f"Coverage summary JSON written to {args.json_output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
