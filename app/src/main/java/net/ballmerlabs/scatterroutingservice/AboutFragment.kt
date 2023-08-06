package net.ballmerlabs.scatterroutingservice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.ballmerlabs.scatterroutingservice.R

@Composable
fun About(paddingValues: PaddingValues) {
    Text(
        modifier = Modifier.padding(paddingValues),
        text = stringResource(R.string.privacy_link)
    )
}