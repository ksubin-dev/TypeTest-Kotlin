import json
import tempfile
import textwrap
import unittest
from pathlib import Path

from coverage_summary import (
    build_html,
    build_json,
    build_markdown,
    low_coverage_classes,
    parse_report,
    read_baseline,
)


SAMPLE_XML = """\
<?xml version="1.0" ?>
<report name="sample">
  <package name="com/example">
    <class name="com/example/SampleViewModel" sourcefilename="SampleViewModel.kt">
      <counter type="INSTRUCTION" missed="10" covered="30"/>
      <counter type="BRANCH" missed="3" covered="1"/>
      <counter type="LINE" missed="2" covered="6"/>
    </class>
    <class name="com/example/CoveredMapper" sourcefilename="CoveredMapper.kt">
      <counter type="INSTRUCTION" missed="0" covered="20"/>
      <counter type="BRANCH" missed="0" covered="0"/>
      <counter type="LINE" missed="0" covered="5"/>
    </class>
    <counter type="INSTRUCTION" missed="10" covered="50"/>
    <counter type="BRANCH" missed="3" covered="1"/>
    <counter type="LINE" missed="2" covered="11"/>
  </package>
  <counter type="INSTRUCTION" missed="10" covered="50"/>
  <counter type="BRANCH" missed="3" covered="1"/>
  <counter type="LINE" missed="2" covered="11"/>
</report>
"""


class CoverageSummaryTest(unittest.TestCase):
    def test_kover_xml에서_coverage_counter를_추출한다(self):
        with tempfile.TemporaryDirectory() as directory:
            xml_path = Path(directory) / "report.xml"
            xml_path.write_text(SAMPLE_XML, encoding="utf-8")

            report = parse_report("focused debug", xml_path)

            self.assertTrue(report.generated)
            self.assertEqual(11, report.counter("LINE").covered)
            self.assertEqual(13, report.counter("LINE").total)
            self.assertAlmostEqual(84.62, report.counter("LINE").percent, places=2)

    def test_이전_기준선이_있으면_변화량을_markdown에_표시한다(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            xml_path = root / "report.xml"
            baseline_path = root / "baseline.json"
            xml_path.write_text(SAMPLE_XML, encoding="utf-8")
            baseline_path.write_text(
                json.dumps(
                    {
                        "reports": {
                            "focused debug": {
                                "LINE": {"percent": 80.0},
                                "BRANCH": {"percent": 20.0},
                                "INSTRUCTION": {"percent": 70.0},
                            }
                        }
                    }
                ),
                encoding="utf-8",
            )

            report = parse_report("focused debug", xml_path)
            markdown = build_markdown(
                reports=[report],
                baseline=read_baseline(baseline_path),
                output_path=root / "summary.md",
                low_threshold=80,
                low_limit=5,
            )

            self.assertIn("+4.62%p", markdown)
            self.assertIn("+5.00%p", markdown)
            self.assertIn("SampleViewModel", markdown)

    def test_리포트가_없어도_요약_생성에_실패하지_않는다(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report = parse_report("focused debug", root / "missing.xml")

            markdown = build_markdown(
                reports=[report],
                baseline={},
                output_path=root / "summary.md",
                low_threshold=80,
                low_limit=5,
            )

            self.assertIn("not generated", markdown)
            self.assertIn("Generate focused Kover XML", markdown)

    def test_낮은_커버리지_class를_line_coverage_기준으로_정렬한다(self):
        with tempfile.TemporaryDirectory() as directory:
            xml_path = Path(directory) / "report.xml"
            xml_path.write_text(textwrap.dedent(SAMPLE_XML), encoding="utf-8")
            report = parse_report("focused debug", xml_path)

            low_items = low_coverage_classes(report, threshold=80, limit=5)

            self.assertEqual(["com.example.SampleViewModel"], [item.name for item in low_items])

    def test_summary_json에_기준선_대비_변화량을_포함한다(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            xml_path = root / "report.xml"
            xml_path.write_text(SAMPLE_XML, encoding="utf-8")
            report = parse_report("focused debug", xml_path)

            summary = build_json(
                reports=[report],
                baseline={
                    "reports": {
                        "focused debug": {
                            "LINE": {"percent": 80.0},
                            "BRANCH": {"percent": 20.0},
                            "INSTRUCTION": {"percent": 70.0},
                        }
                    }
                },
                low_threshold=80,
                low_limit=5,
            )

            self.assertEqual(4.62, summary["reports"]["focused debug"]["LINE"]["deltaPercentPoint"])
            self.assertEqual(5.0, summary["reports"]["focused debug"]["BRANCH"]["deltaPercentPoint"])

    def test_html_품질_리포트에_테스트_보완_정보를_표시한다(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            xml_path = root / "report.xml"
            xml_path.write_text(SAMPLE_XML, encoding="utf-8")
            report = parse_report("focused debug", xml_path)
            summary = build_json(
                reports=[report],
                baseline={"reports": {"focused debug": {"LINE": {"percent": 80.0}}}},
                low_threshold=80,
                low_limit=5,
            )

            html = build_html(summary)

            self.assertIn("Coverage Quality Report", html)
            self.assertIn("SampleViewModel", html)
            self.assertIn("+4.62%p", html)
            self.assertIn("Next Test Candidates", html)
            self.assertIn("상태 전이", html)

    def test_full_reference가_없어도_html_생성에_실패하지_않는다(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            focused_xml = root / "focused.xml"
            focused_xml.write_text(SAMPLE_XML, encoding="utf-8")
            reports = [
                parse_report("focused debug", focused_xml),
                parse_report("full reference", root / "missing.xml"),
            ]
            summary = build_json(
                reports=reports,
                baseline={},
                low_threshold=80,
                low_limit=5,
            )

            html = build_html(summary)

            self.assertIn("full reference", html)
            self.assertIn("not generated in this run", html)


if __name__ == "__main__":
    unittest.main()
