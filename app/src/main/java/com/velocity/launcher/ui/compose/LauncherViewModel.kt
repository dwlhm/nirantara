package com.velocity.launcher.ui.compose

import android.app.Application
import android.content.pm.ShortcutInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val apps = repository.loadApps()
            
            // For the sake of the concept brief, we'll divide apps into some arbitrary folders
            // and keep a few in the neutral area.
            
            val neutralCount = (apps.size * 0.1).toInt().coerceAtMost(5)
            val neutralApps = apps.take(neutralCount)
            val remainingApps = apps.drop(neutralCount)
            
            val categories = listOf("Communication", "Media", "Work", "Games", "System", "Utilities", "Uncategorized")
            
            val folders = mutableListOf<FolderState>()
            val appsPerFolder = if (categories.isNotEmpty()) remainingApps.size / categories.size else remainingApps.size
            
            categories.forEachIndexed { index, name ->
                val folderApps = if (index == categories.size - 1) {
                    remainingApps.drop(index * appsPerFolder)
                } else {
                    remainingApps.drop(index * appsPerFolder).take(appsPerFolder)
                }
                
                if (folderApps.isNotEmpty()) {
                    folders.add(
                        FolderState(
                            name = name,
                            cards = folderApps.map { 
                                CardState(
                                    app = it,
                                    shortcuts = repository.getShortcuts(it)
                                ) 
                            }
                        )
                    )
                }
            }

            _state.update { 
                it.copy(
                    folders = folders,
                    neutralApps = neutralApps,
                    isLoading = false
                )
            }
        }
    }

    fun updateCardSize(folderId: String, cardId: String, widthCells: Int, heightCells: Int) {
        _state.update { currentState ->
            val updatedFolders = currentState.folders.map { folder ->
                if (folder.id == folderId) {
                    folder.copy(
                        cards = folder.cards.map { card ->
                            if (card.id == cardId) {
                                card.copy(widthCells = widthCells, heightCells = heightCells)
                            } else card
                        }
                    )
                } else folder
            }
            currentState.copy(folders = updatedFolders)
        }
    }

    fun setWidgetConfigured(
        folderId: String,
        cardId: String,
        appWidgetId: Int,
        widthCells: Int? = null,
        heightCells: Int? = null
    ) {
        _state.update { currentState ->
            val updatedFolders = currentState.folders.map { folder ->
                if (folder.id == folderId) {
                    folder.copy(
                        cards = folder.cards.map { card ->
                            if (card.id == cardId) {
                                card.copy(
                                    isWidgetConfigured = true,
                                    appWidgetId = appWidgetId,
                                    widthCells = widthCells ?: card.widthCells,
                                    heightCells = heightCells ?: card.heightCells
                                )
                            } else card
                        }
                    )
                } else folder
            }
            currentState.copy(folders = updatedFolders)
        }
    }

    fun moveFromNeutralToFolder(app: AppModel) {
        _state.update { currentState ->
            val newNeutralApps = currentState.neutralApps.filter { it.packageName != app.packageName }
            
            // Find or create "Uncategorized" folder
            val uncategorizedName = "Uncategorized"
            var found = false
            val updatedFolders = currentState.folders.map { folder ->
                if (folder.name == uncategorizedName) {
                    found = true
                    folder.copy(cards = folder.cards + CardState(app = app, shortcuts = repository.getShortcuts(app)))
                } else {
                    folder
                }
            }.toMutableList()
            
            if (!found) {
                updatedFolders.add(
                    FolderState(name = uncategorizedName, cards = listOf(CardState(app = app, shortcuts = repository.getShortcuts(app))))
                )
            }
            
            currentState.copy(
                neutralApps = newNeutralApps,
                folders = updatedFolders
            )
        }
    }

    fun launchApp(app: AppModel) {
        repository.launchApp(app)
    }
    
    fun getIcon(app: AppModel) = repository.getIcon(app)

    fun getShortcutIcon(shortcut: ShortcutInfo) = repository.getShortcutIcon(shortcut)

    fun launchShortcut(shortcut: ShortcutInfo) {
        repository.launchShortcut(shortcut)
    }
}
