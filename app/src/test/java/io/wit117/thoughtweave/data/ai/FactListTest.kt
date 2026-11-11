package io.wit117.thoughtweave.data.ai

import io.wit117.thoughtweave.data.ai.engine.InferenceEngine
import io.wit117.thoughtweave.data.ai.engine.InferencePrompt
import io.wit117.thoughtweave.data.ai.parse.factList
import io.wit117.thoughtweave.data.repository.MemoSynthesisRepositoryImpl
import io.wit117.thoughtweave.domain.model.AiLanguagePlan
import io.wit117.thoughtweave.domain.model.AppLanguages
import io.wit117.thoughtweave.domain.model.Memo
import io.wit117.thoughtweave.domain.model.MemoOrigin
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FactListTest {
    private fun memo(content: String, title: String = "") =
        Memo(1, title, content, Instant.EPOCH, Instant.EPOCH, false, MemoOrigin.MANUAL)

    @Test
    fun splitsLinesAndSentencesKeepingWording() {
        assertEquals(
            listOf("11.25 오후 2시 3층 회의실.", "김대리, 박과장 참석.", "예산안 검토."),
            factList(listOf(memo("11.25 오후 2시 3층 회의실. 김대리, 박과장 참석. 예산안 검토."))),
        )
        assertEquals(
            listOf("기획 회의는 3층 대회의실", "노트북 지참"),
            factList(listOf(memo("기획 회의는 3층 대회의실\n노트북 지참"))),
        )
    }

    @Test
    fun keepsDecimalsDatesAndAbbreviationsWhole() {
        assertEquals(listOf("회의 3.5시간 소요, 12.2 발표"), factList(listOf(memo("회의 3.5시간 소요, 12.2 발표"))))
        assertEquals(listOf("Release on Dec. 4 at 10am."), factList(listOf(memo("Release on Dec. 4 at 10am."))))
        assertEquals(
            listOf("Where should we go?", "Maybe the noodle place."),
            factList(listOf(memo("Where should we go? Maybe the noodle place."))),
        )
    }

    @Test
    fun removesDuplicatesAcrossMemosAndBlankLines() {
        assertEquals(
            listOf("노트북 지참", "회의 자료 20부 출력"),
            factList(listOf(memo("노트북 지참\n\n"), memo("노트북  지참.\n회의 자료 20부 출력"))),
        )
    }

    @Test
    fun stripsExistingBulletMarks() {
        assertEquals(listOf("우유 사기", "책 반납"), factList(listOf(memo("- 우유 사기\n• 책 반납"))))
    }

    @Test
    fun summaryIsAiTextThenTheOriginalItems() {
        val engine = object : InferenceEngine {
            override fun generate(prompt: InferencePrompt): Flow<String> = flowOf("회의는 11월 25일", " 진행.")
        }
        val memos = listOf(memo("11.25 오후 2시 기획 회의.\n노트북 지참"), memo("기획 회의는 3층 대회의실"))
        val plan = AiLanguagePlan(AppLanguages.KOREAN, AppLanguages.KOREAN)

        val text = runBlocking { MemoSynthesisRepositoryImpl(engine).summarize(memos, plan).toList() }.joinToString("")

        assertEquals(
            "회의는 11월 25일 진행.\n\n원문 항목\n- 11.25 오후 2시 기획 회의.\n- 노트북 지참\n- 기획 회의는 3층 대회의실",
            text,
        )
    }

    @Test
    fun aiPartDropsTheOriginalItems() {
        val repository = MemoSynthesisRepositoryImpl(object : InferenceEngine {
            override fun generate(prompt: InferencePrompt): Flow<String> = flowOf()
        })
        assertEquals(
            "회의는 11월 25일 진행.",
            repository.aiPart("회의는 11월 25일 진행.\n\n원문 항목\n- 12.2 발표", AppLanguages.KOREAN),
        )
        assertEquals("Meeting.", repository.aiPart("Meeting.\n\nOriginal items\n- Dec 2 talk", AppLanguages.ENGLISH))
        assertEquals("heading removed\n- 12.2", repository.aiPart("heading removed\n- 12.2", AppLanguages.KOREAN))
    }
}
