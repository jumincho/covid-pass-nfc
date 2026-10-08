package com.jumincho.cvpass.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.jumincho.cvpass.R
import com.jumincho.cvpass.core.business.BusinessNumber

/**
 * A field for a business registration number. [value] holds digits only and is shown as
 * `123-45-67890`; [problem], if any, replaces the format hint below the field.
 */
@Composable
fun BusinessNumberField(
    value: String,
    problem: BusinessNumber.Problem?,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val error = when (problem) {
        BusinessNumber.Problem.WRONG_FORMAT -> stringResource(R.string.field_business_number_format)
        BusinessNumber.Problem.CHECKSUM_MISMATCH -> stringResource(R.string.field_business_number_checksum)
        null -> null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_business_number)) },
        supportingText = { Text(error ?: stringResource(R.string.field_business_number_hint)) },
        isError = error != null,
        singleLine = true,
        visualTransformation = remember { FormattedDigitsTransformation(BusinessNumber::formatPartial) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = keyboardActions,
        modifier = modifier,
    )
}
