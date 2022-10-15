/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pspdfkit.navigator

import android.content.Context
import org.readium.adapters.pspdfkit.document.PsPdfKitDocumentFactory
import org.readium.r2.navigator.SimplePresentation
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.pdf.PdfDocumentFragmentInput
import org.readium.r2.navigator.pdf.PdfEngineProvider
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.pdf.cachedIn

@ExperimentalReadiumApi
class PsPdfKitEngineProvider(
    private val context: Context,
    private val defaults: PsPdfKitDefaults = PsPdfKitDefaults(),
    private val editorConfiguration: PsPdfKitPreferencesEditor.Configuration = PsPdfKitPreferencesEditor.Configuration()
) : PdfEngineProvider<PsPdfKitSettings, PsPdfKitPreferences, PsPdfKitPreferencesEditor> {

    override suspend fun createDocumentFragment(
        input: PdfDocumentFragmentInput<PsPdfKitSettings>
    ): PdfDocumentFragment<PsPdfKitSettings> {

        val publication = input.publication
        val document = PsPdfKitDocumentFactory(context)
            .cachedIn(publication)
            .open(publication.get(input.link), null)

        return PsPdfKitDocumentFragment(
            publication = publication,
            document = document,
            initialPageIndex = input.initialPageIndex,
            settings = input.settings,
            listener = input.listener
        )
    }

    override fun computeSettings(metadata: Metadata, preferences: PsPdfKitPreferences): PsPdfKitSettings {
        val settingsPolicy = PsPdfKitSettingsResolver(defaults, metadata)
        return settingsPolicy.settings(preferences)
    }

    override fun computePresentation(settings: PsPdfKitSettings): VisualNavigator.Presentation =
        SimplePresentation(
            readingProgression = settings.readingProgression,
            scroll = settings.scroll,
            axis =  if (settings.scroll) settings.scrollAxis else Axis.HORIZONTAL
        )

    override fun createPreferenceEditor(
        publication: Publication,
        currentSettings: PsPdfKitSettings,
        currentPreferences: PsPdfKitPreferences
    ): PsPdfKitPreferencesEditor =
        PsPdfKitPreferencesEditor(
            currentSettings = currentSettings,
            initialPreferences = currentPreferences,
            publicationMetadata = publication.metadata,
            defaults = defaults,
            configuration = editorConfiguration
        )

    override fun createEmptyPreferences(): PsPdfKitPreferences =
        PsPdfKitPreferences()
}
