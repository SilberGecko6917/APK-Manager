package me.geckotv.apkmanager.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.data.repository.AddAppOptions
import me.geckotv.apkmanager.core.data.repository.DeviceInfo
import me.geckotv.apkmanager.core.data.repository.InstalledAppsRepository
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.model.GitHubRepositoryParser
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceResult
import javax.inject.Inject

/** Looks up a GitHub repository and lets the user review it before it is tracked. */
@HiltViewModel
class AddAppViewModel
    @Inject
    constructor(
        private val trackedAppsRepository: TrackedAppsRepository,
        private val installedAppsRepository: InstalledAppsRepository,
        private val deviceInfo: DeviceInfo,
    ) : ViewModel() {
        private val state = MutableStateFlow(AddAppUiState())
        private val eventChannel = Channel<AddAppEvent>(Channel.BUFFERED)
        private var lookUpJob: Job? = null

        val uiState: StateFlow<AddAppUiState> = state.asStateFlow()

        /** One-off events, e.g. that the app was added. */
        val events: Flow<AddAppEvent> = eventChannel.receiveAsFlow()

        fun setInput(input: String) {
            state.update { it.copy(input = input, inputError = null) }
        }

        /** Validates the input and looks up the repository. */
        fun lookUp() {
            val repository = GitHubRepositoryParser.parse(state.value.input)
            if (repository == null) {
                state.update { it.copy(inputError = AddAppInputError.Invalid) }
            } else {
                lookUpJob?.cancel()
                lookUpJob =
                    viewModelScope.launch {
                        if (trackedAppsRepository.findTrackedApp(repository) != null) {
                            state.update { it.copy(inputError = AddAppInputError.AlreadyTracked) }
                        } else {
                            state.update { it.copy(step = AddAppStep.Loading, inputError = null) }
                            val step =
                                when (val result = trackedAppsRepository.lookUpRepository(repository)) {
                                    is SourceResult.Failure -> AddAppStep.Failed(result.error)
                                    is SourceResult.Success -> previewStep(result.data)
                                }
                            state.update { it.copy(step = step) }
                        }
                    }
            }
        }

        /** Goes one step back: closes the package picker, or returns from the preview or an error to the input. */
        fun back() {
            val step = state.value.step
            if (step is AddAppStep.Preview && step.isPickingPackage) {
                state.updatePreview { it.copy(isPickingPackage = false) }
            } else {
                lookUpJob?.cancel()
                state.update { it.copy(step = AddAppStep.Input) }
            }
        }

        /** Forgets everything, so the next time the sheet opens it starts with an empty input. */
        fun reset() {
            lookUpJob?.cancel()
            state.value = AddAppUiState()
        }

        fun setIncludePrereleases(includePrereleases: Boolean) {
            state.updatePreview { it.copy(includePrereleases = includePrereleases) }
        }

        /** The user picked [assetName] among several APKs. */
        fun selectAsset(assetName: String) {
            state.updatePreview { it.copy(chosenAssetName = assetName) }
        }

        fun showPackagePicker() {
            state.updatePreview { it.copy(isPickingPackage = true) }
        }

        /** Links the app to the installed [packageName], or to nothing with `null`, and closes the picker. */
        fun selectPackage(packageName: String?) {
            state.updatePreview { it.copy(packageName = packageName, isPickingPackage = false) }
        }

        /** Tracks the repository with the choices made in the preview. */
        fun add() {
            val step = state.value.step
            if (step is AddAppStep.Preview && !step.isAdding) {
                state.updatePreview { it.copy(isAdding = true) }
                viewModelScope.launch {
                    trackedAppsRepository.addApp(
                        preview = step.preview,
                        options =
                            AddAppOptions(
                                packageName = step.packageName,
                                includePrereleases = step.includePrereleases,
                                preferredAssetName = step.preferredAssetName,
                            ),
                    )
                    eventChannel.send(AddAppEvent.Added(step.displayName))
                    reset()
                }
            }
        }

        private suspend fun previewStep(preview: RepositoryPreview): AddAppStep.Preview {
            val installedApps = installedAppsRepository.installedApps()
            return AddAppStep.Preview(
                preview = preview,
                installedApps = installedApps,
                supportedAbis = deviceInfo.supportedAbis,
                packageName = suggestInstalledApp(preview.repository.name, installedApps)?.packageName,
            )
        }
    }

private fun MutableStateFlow<AddAppUiState>.updatePreview(transform: (AddAppStep.Preview) -> AddAppStep.Preview) {
    update { current ->
        val step = current.step
        if (step is AddAppStep.Preview) current.copy(step = transform(step)) else current
    }
}
