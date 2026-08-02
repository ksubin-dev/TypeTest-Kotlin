#!/usr/bin/env python3
"""Generate Markdown, JSON, and HTML summaries from Kover XML reports."""

from __future__ import annotations

import argparse
import html
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


def delta_value(current: float, previous: float | None) -> float | None:
    if previous is None:
        return None
    delta = current - previous
    if abs(delta) < 0.005:
        return 0.0
    return round(delta, 2)


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


def counter_payload(counter: Counter, previous: float | None) -> dict[str, int | float | None]:
    return {
        **counter.as_dict(),
        "baselinePercent": None if previous is None else round(previous, 2),
        "deltaPercentPoint": delta_value(counter.percent, previous),
    }


def build_json(
    reports: list[CoverageReport],
    baseline: dict,
    low_threshold: float,
    low_limit: int,
) -> dict:
    focused = next((report for report in reports if report.label == "focused debug"), reports[0])
    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "qualitySignal": "focused debug",
        "reports": {
            report.label: {
                "xmlPath": report.xml_path.as_posix(),
                "generated": report.generated,
                **{
                    counter_type: counter_payload(
                        report.counter(counter_type),
                        baseline_percent(baseline, report.label, counter_type),
                    )
                    for counter_type in COUNTER_TYPES
                },
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


def format_json_percent(counter: dict) -> str:
    return f"{float(counter.get('percent', 0.0)):.2f}%"


def format_json_fraction(counter: dict) -> str:
    return f"{int(counter.get('covered', 0))}/{int(counter.get('total', 0))}"


def format_json_delta(counter: dict) -> str:
    delta = counter.get("deltaPercentPoint")
    if delta is None:
        return "n/a"
    value = float(delta)
    sign = "+" if value >= 0 else ""
    return f"{sign}{value:.2f}%p"


def score_class(percent: float) -> str:
    if percent >= 80:
        return "good"
    if percent >= 50:
        return "warn"
    return "bad"


def build_html(summary: dict) -> str:
    generated_at = html.escape(str(summary.get("generatedAt", "unknown")))
    reports = summary.get("reports", {})
    low_areas = summary.get("lowCoverageAreas", [])
    focused = reports.get("focused debug", {})
    focused_line = focused.get("LINE", {})
    focused_branch = focused.get("BRANCH", {})

    def metric_card(title: str, counter: dict, description: str) -> str:
        percent = float(counter.get("percent", 0.0))
        css_class = score_class(percent)
        return f"""
        <section class="metric {css_class}">
          <div class="metric-label">{html.escape(title)}</div>
          <div class="metric-value">{format_json_percent(counter)}</div>
          <div class="metric-meta">{format_json_fraction(counter)} lines · {format_json_delta(counter)}</div>
          <div class="bar" aria-hidden="true"><span style="width: {percent:.2f}%"></span></div>
          <p>{html.escape(description)}</p>
        </section>
        """

    def count_card(title: str, count: int, description: str) -> str:
        css_class = "good" if count == 0 else "bad"
        label = "area" if count == 1 else "areas"
        return f"""
        <section class="metric {css_class}">
          <div class="metric-label">{html.escape(title)}</div>
          <div class="metric-value">{count}</div>
          <div class="metric-meta">{html.escape(label)} below threshold</div>
          <div class="bar" aria-hidden="true"><span style="width: {'100' if count == 0 else '18'}%"></span></div>
          <p>{html.escape(description)}</p>
        </section>
        """

    report_rows = []
    for label, report in reports.items():
        if not report.get("generated", False):
            report_rows.append(
                f"""
                <tr>
                  <td>{html.escape(label)}</td>
                  <td colspan="6" class="muted">not generated in this run</td>
                </tr>
                """
            )
            continue
        cells = [f"<td>{html.escape(label)}</td>"]
        for counter_type in COUNTER_TYPES:
            counter = report.get(counter_type, {})
            cells.append(
                f"""
                <td>
                  <strong>{format_json_percent(counter)}</strong>
                  <span>{format_json_fraction(counter)}</span>
                </td>
                <td>{format_json_delta(counter)}</td>
                """
            )
        report_rows.append(f"<tr>{''.join(cells)}</tr>")

    low_rows = []
    for item in low_areas:
        line = item.get("line", {})
        branch = item.get("branch", {})
        instruction = item.get("instruction", {})
        low_rows.append(
            f"""
            <tr>
              <td><code>{html.escape(str(item.get("className", "")))}</code></td>
              <td>{html.escape(str(item.get("sourceFile", "")))}</td>
              <td>{format_json_percent(line)} <span>{format_json_fraction(line)}</span></td>
              <td>{format_json_percent(branch)} <span>{format_json_fraction(branch)}</span></td>
              <td>{format_json_percent(instruction)} <span>{format_json_fraction(instruction)}</span></td>
            </tr>
            """
        )

    recommendations = []
    seen: set[str] = set()
    for item in low_areas:
        recommendation = str(item.get("recommendation", "")).strip()
        class_name = str(item.get("className", "")).strip()
        key = f"{class_name}:{recommendation}"
        if not recommendation or key in seen:
            continue
        seen.add(key)
        recommendations.append(
            f"<li><code>{html.escape(class_name)}</code><span>{html.escape(recommendation)}</span></li>"
        )

    low_area_markup = (
        "\n".join(low_rows)
        if low_rows
        else '<tr><td colspan="5" class="muted">No focused classes are below the threshold.</td></tr>'
    )
    recommendation_markup = (
        "\n".join(recommendations)
        if recommendations
        else "<li><span>No immediate low-coverage candidate found. Consider enabling a focused coverage gate.</span></li>"
    )

    return f"""<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Coverage Quality Report</title>
  <style>
    :root {{
      color-scheme: light;
      --ink: #20242c;
      --muted: #647084;
      --line: #d7dde8;
      --panel: #ffffff;
      --soft: #f5f7fb;
      --good: #1b7f4b;
      --warn: #a96500;
      --bad: #c7352b;
      --accent: #255c99;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background: var(--soft);
      color: var(--ink);
      font-family: Arial, "Noto Sans KR", sans-serif;
      line-height: 1.55;
    }}
    main {{
      width: min(1120px, calc(100% - 32px));
      margin: 32px auto;
    }}
    header {{
      border-bottom: 1px solid var(--line);
      margin-bottom: 24px;
      padding-bottom: 20px;
    }}
    h1, h2 {{ margin: 0; letter-spacing: 0; }}
    h1 {{ font-size: 32px; }}
    h2 {{ font-size: 21px; margin-bottom: 14px; }}
    p {{ margin: 8px 0 0; }}
    .subtitle {{ color: var(--muted); max-width: 760px; }}
    .meta {{
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 16px;
    }}
    .pill {{
      border: 1px solid var(--line);
      background: #fff;
      border-radius: 999px;
      color: var(--muted);
      font-size: 13px;
      padding: 5px 10px;
    }}
    .grid {{
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 12px;
      margin: 18px 0 26px;
    }}
    .metric, .section {{
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 18px;
    }}
    .metric-label {{ color: var(--muted); font-size: 13px; font-weight: 700; text-transform: uppercase; }}
    .metric-value {{ font-size: 34px; font-weight: 800; margin-top: 6px; }}
    .metric-meta {{ color: var(--muted); font-size: 13px; margin-top: 2px; }}
    .metric.good .metric-value {{ color: var(--good); }}
    .metric.warn .metric-value {{ color: var(--warn); }}
    .metric.bad .metric-value {{ color: var(--bad); }}
    .metric.good .bar span {{ background: var(--good); }}
    .metric.warn .bar span {{ background: var(--warn); }}
    .metric.bad .bar span {{ background: var(--bad); }}
    .bar {{ height: 8px; border-radius: 999px; background: #e8edf5; margin-top: 12px; overflow: hidden; }}
    .bar span {{ display: block; height: 100%; background: currentColor; border-radius: inherit; }}
    table {{
      width: 100%;
      border-collapse: collapse;
      background: #fff;
      border: 1px solid var(--line);
      border-radius: 8px;
      overflow: hidden;
    }}
    th, td {{ border-bottom: 1px solid var(--line); padding: 12px; text-align: left; vertical-align: top; }}
    th {{ background: #f0f3f8; font-size: 13px; }}
    tr:last-child td {{ border-bottom: 0; }}
    td span {{ color: var(--muted); display: block; font-size: 12px; }}
    code {{
      background: #eef2f7;
      border-radius: 5px;
      padding: 2px 5px;
      word-break: break-word;
    }}
    .section {{ margin-top: 16px; }}
    .recommendations {{
      display: grid;
      gap: 10px;
      list-style: none;
      margin: 0;
      padding: 0;
    }}
    .recommendations li {{
      border: 1px solid var(--line);
      border-left: 4px solid var(--accent);
      border-radius: 8px;
      background: #fff;
      padding: 12px;
    }}
    .recommendations span {{ display: block; margin-top: 6px; }}
    .muted {{ color: var(--muted); }}
    .note {{
      border-left: 4px solid var(--accent);
      background: #f6f9fd;
      padding: 12px 14px;
      border-radius: 6px;
    }}
    .table-wrap {{ overflow-x: auto; }}
    @media (max-width: 760px) {{
      main {{ width: min(100% - 20px, 1120px); margin: 20px auto; }}
      h1 {{ font-size: 26px; }}
      .grid {{ grid-template-columns: 1fr; }}
    }}
  </style>
</head>
<body>
  <main>
    <header>
      <h1>Coverage Quality Report</h1>
      <p class="subtitle">테스트 자동화와 코드 품질 개선을 위해 focused coverage, 기준선 대비 변화량, 낮은 coverage 영역, 다음 테스트 후보를 정리한 리포트입니다.</p>
      <div class="meta">
        <span class="pill">generated: {generated_at}</span>
        <span class="pill">quality signal: {html.escape(str(summary.get("qualitySignal", "focused debug")))}</span>
        <span class="pill">full reference is informational</span>
      </div>
    </header>

    <section>
      <h2>Quality Signals</h2>
      <div class="grid">
        {metric_card("Focused LINE", focused_line, "핵심 production code의 실행 라인 보호 수준입니다.")}
        {metric_card("Focused BRANCH", focused_branch, "조건 분기와 예외 흐름 테스트가 충분한지 보는 신호입니다.")}
        {count_card("Low Coverage Areas", len(low_areas), "테스트 보완 우선순위가 필요한 class 수입니다.")}
      </div>
    </section>

    <section class="section">
      <h2>Current Coverage</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Report</th>
              <th>LINE</th>
              <th>Delta</th>
              <th>BRANCH</th>
              <th>Delta</th>
              <th>INSTRUCTION</th>
              <th>Delta</th>
            </tr>
          </thead>
          <tbody>
            {''.join(report_rows)}
          </tbody>
        </table>
      </div>
    </section>

    <section class="section">
      <h2>Low Coverage Areas</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Class</th>
              <th>Source</th>
              <th>LINE</th>
              <th>BRANCH</th>
              <th>INSTRUCTION</th>
            </tr>
          </thead>
          <tbody>
            {low_area_markup}
          </tbody>
        </table>
      </div>
    </section>

    <section class="section">
      <h2>Next Test Candidates</h2>
      <ul class="recommendations">
        {recommendation_markup}
      </ul>
    </section>

    <section class="section">
      <h2>Analysis Notes</h2>
      <p class="note">focused debug coverage를 주요 품질 신호로 보고, full reference coverage는 Compose UI와 Android 연결 코드가 포함된 참고 지표로만 사용합니다. 80% gate를 적용하기 전에는 ResultCalculator와 ViewModel 테스트를 우선 보강합니다.</p>
    </section>
  </main>
</body>
</html>
"""


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
    parser.add_argument("--html-output", type=Path, default=Path("build/reports/coverage-summary/coverage-report.html"))
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
    summary_json = build_json(reports, baseline, args.low_threshold, args.low_limit)
    write_text(args.output, markdown)
    write_text(args.json_output, json.dumps(summary_json, ensure_ascii=False, indent=2) + "\n")
    write_text(args.html_output, build_html(summary_json))

    if args.append_step_summary:
        summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary_path:
            with Path(summary_path).open("a", encoding="utf-8") as summary:
                summary.write(markdown)
                summary.write("\n")

    print(f"Coverage summary written to {args.output}")
    print(f"Coverage summary JSON written to {args.json_output}")
    print(f"Coverage quality HTML written to {args.html_output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
