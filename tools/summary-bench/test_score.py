"""Run: python -I test_score.py (standard library only)."""
import unittest

from score import dates_in, has_fact, kept_as_question, language_ok, load_sets, main, score


class DatesTest(unittest.TestCase):
    def test_formats(self):
        self.assertEqual(dates_in("12월 5일(토), 12.1까지, 12/7"), {"12-05", "12-01", "12-07"})
        self.assertEqual(dates_in("2026.12.5 워크숍"), {"12-05"})
        self.assertEqual(dates_in("베타 12/1~12/7, 스토어 12.8."), {"12-01", "12-07", "12-08"})
        self.assertEqual(dates_in("Jan 9, 12 January, December 3rd"), {"01-09", "01-12", "12-03"})

    def test_not_dates(self):
        self.assertEqual(dates_in("12일 후, 다음 주 수요일, next Friday"), set())
        self.assertEqual(dates_in("4,900원, 7:40 am, 6:30, 600,000 won"), set())

    def test_month_dot_day_is_ambiguous(self):
        # Like the app, "3.14" reads as March 14. The sets avoid decimals so this cannot skew scores.
        self.assertEqual(dates_in("3.14"), {"03-14"})
        self.assertEqual(dates_in("the market 5 and we decide 3 things, may 40"), set())


class FactTest(unittest.TestCase):
    def test_digits_do_not_run_on(self):
        self.assertFalse(has_fact("출시일은 12.15", [["12.1"]]))
        self.assertFalse(has_fact("118명", [["18명"]]))
        self.assertTrue(has_fact("12.1까지", [["12.1"]]))

    def test_spacing_and_case(self):
        self.assertTrue(has_fact("숙소는 2인 1실", [["2인1실"]]))
        self.assertTrue(has_fact("참석 18 명", [["18명"]]))
        self.assertTrue(has_fact("Near HAEUNDAE", [["haeundae"]]))

    def test_every_group_needed(self):
        self.assertFalse(has_fact("케이크 픽업", [["민지"], ["케이크"]]))


class QuestionTest(unittest.TestCase):
    def test_question_kept(self):
        self.assertTrue(kept_as_question("가격은 월 4,900원으로 할까?", [["4,900"]]))
        self.assertTrue(kept_as_question("가격(월 4,900원) 여부 미정.", [["4,900"]]))
        self.assertTrue(kept_as_question("Car or subway: not decided yet.", [["car"], ["subway"]]))

    def test_answered_or_statement(self):
        self.assertFalse(kept_as_question("가격은 월 4,900원이다.", [["4,900"]]))
        self.assertFalse(kept_as_question("We will take the subway, not a car.", [["car"], ["subway"]]))
        self.assertFalse(kept_as_question("12.1까지 4,900원 결제.", [["4,900"]]))


class ScoreTest(unittest.TestCase):
    def test_language(self):
        self.assertTrue(language_ok("출시는 12월 15일, Jira 사용", "ko"))
        self.assertFalse(language_ok("The launch is on December 15.", "ko"))
        self.assertFalse(language_ok("부산 여행 1월 9일", "en"))

    def test_changed_date_is_missing_and_added(self):
        s = load_sets()["ko-3"]
        sc = score(s, "출시는 12월 16일. 베타 12/1~12/7, 스토어 12.8까지.")
        self.assertEqual(sc.dates_missing, ["12-15"])
        self.assertEqual(sc.dates_added, ["12-16"])
        self.assertIn("launch-date", sc.lost)

    def test_self_check_passes(self):
        self.assertEqual(main(["--self-check"]), 0)


if __name__ == "__main__":
    unittest.main()
