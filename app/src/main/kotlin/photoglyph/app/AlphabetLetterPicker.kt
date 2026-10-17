package photoglyph.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal data class LetterCasePickerState(
    val script: HandwritingScript,
    val selectedCharacter: String?,
    val savedLabels: Set<String>,
    val enabled: Boolean,
)

internal data class LetterCasePickerActions(
    val onScriptChange: (HandwritingScript) -> Unit,
    val onCharacterSelected: (String) -> Unit,
)

@Composable
internal fun LetterCasePicker(
    state: LetterCasePickerState,
    actions: LetterCasePickerActions,
) {
    val progress = AlphabetLetterForms.progress(state.script, state.savedLabels)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CASE_PICKER_GAP_DP.dp),
    ) {
        Text(stringResource(R.string.alphabet_case_picker_title))
        Row(horizontalArrangement = Arrangement.spacedBy(CASE_PICKER_GAP_DP.dp)) {
            FilterChip(
                selected = state.script == HandwritingScript.CYRILLIC,
                enabled = state.enabled,
                onClick = { actions.onScriptChange(HandwritingScript.CYRILLIC) },
                label = { Text(stringResource(R.string.alphabet_script_cyrillic)) },
            )
            FilterChip(
                selected = state.script == HandwritingScript.LATIN,
                enabled = state.enabled,
                onClick = { actions.onScriptChange(HandwritingScript.LATIN) },
                label = { Text(stringResource(R.string.alphabet_script_latin)) },
            )
        }
        Text(
            stringResource(
                R.string.alphabet_case_progress,
                progress.uppercaseAdded,
                progress.uppercaseTotal,
                progress.lowercaseAdded,
                progress.lowercaseTotal,
            ),
        )
        Text(stringResource(R.string.alphabet_uppercase_forms))
        LetterFormChips(
            forms = AlphabetLetterForms.uppercase(state.script),
            selectedCharacter = state.selectedCharacter,
            savedLabels = state.savedLabels,
            enabled = state.enabled,
            onCharacterSelected = actions.onCharacterSelected,
        )
        Text(stringResource(R.string.alphabet_lowercase_forms))
        LetterFormChips(
            forms = AlphabetLetterForms.lowercase(state.script),
            selectedCharacter = state.selectedCharacter,
            savedLabels = state.savedLabels,
            enabled = state.enabled,
            onCharacterSelected = actions.onCharacterSelected,
        )
        state.selectedCharacter?.let { character ->
            Text(stringResource(R.string.alphabet_case_selected, character))
        }
    }
}

@Composable
private fun LetterFormChips(
    forms: List<String>,
    selectedCharacter: String?,
    savedLabels: Set<String>,
    enabled: Boolean,
    onCharacterSelected: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CASE_PICKER_GAP_DP.dp),
        verticalArrangement = Arrangement.spacedBy(CASE_PICKER_GAP_DP.dp),
    ) {
        forms.forEach { character ->
            val alreadyAdded = character in savedLabels
            FilterChip(
                selected = character == selectedCharacter,
                enabled = enabled && !alreadyAdded,
                onClick = { onCharacterSelected(character) },
                label = { Text(if (alreadyAdded) "$character ✓" else character) },
            )
        }
    }
}

private const val CASE_PICKER_GAP_DP = 8
