import json
import tempfile
import textwrap
import unittest
from pathlib import Path

from coverage_summary import build_markdown, low_coverage_classes, parse_report, read_baseline


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


if __name__ == "__main__":
    unittest.main()
