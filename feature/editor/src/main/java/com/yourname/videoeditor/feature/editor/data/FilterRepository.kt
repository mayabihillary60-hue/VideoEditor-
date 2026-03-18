package com.yourname.videoeditor.feature.editor.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourname.videoeditor.feature.editor.model.FilterPreset
import com.yourname.videoeditor.feature.editor.model.DefaultPresets
import com.yourname.videoeditor.feature.editor.model.PresetCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FilterRepository(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("filter_presets", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val customPresetsKey = "custom_presets"
    private val recentPresetsKey = "recent_presets"
    private val favoritePresetsKey = "favorite_presets"

    // Get all presets (default + custom)
    fun getAllPresets(): Flow<List<FilterPreset>> = flow {
        val customPresets = getCustomPresets()
        val allPresets = DefaultPresets.allPresets + customPresets
        emit(allPresets)
    }

    // Get presets by category
    fun getPresetsByCategory(category: PresetCategory): Flow<List<FilterPreset>> = flow {
        val allPresets = getAllPresets()
        val filtered = allPresets.filter { it.category == category }
        emit(filtered)
    }

    // Get default presets
    fun getDefaultPresets(): List<FilterPreset> {
        return DefaultPresets.allPresets
    }

    // Get custom presets
    fun getCustomPresets(): List<FilterPreset> {
        val json = sharedPrefs.getString(customPresetsKey, null) ?: return emptyList()
        val type = object : TypeToken<List<FilterPreset>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // Save custom preset
    fun saveCustomPreset(preset: FilterPreset) {
        val currentPresets = getCustomPresets().toMutableList()
        currentPresets.add(preset)
        val json = gson.toJson(currentPresets)
        sharedPrefs.edit().putString(customPresetsKey, json).apply()
    }

    // Update existing preset
    fun updatePreset(preset: FilterPreset) {
        val currentPresets = getCustomPresets().toMutableList()
        val index = currentPresets.indexOfFirst { it.id == preset.id }
        if (index != -1) {
            currentPresets[index] = preset
            val json = gson.toJson(currentPresets)
            sharedPrefs.edit().putString(customPresetsKey, json).apply()
        }
    }

    // Delete custom preset
    fun deletePreset(presetId: String) {
        val currentPresets = getCustomPresets().toMutableList()
        currentPresets.removeAll { it.id == presetId }
        val json = gson.toJson(currentPresets)
        sharedPrefs.edit().putString(customPresetsKey, json).apply()
    }

    // Get preset by ID
    fun getPresetById(presetId: String): FilterPreset? {
        // Check default presets
        DefaultPresets.allPresets.find { it.id == presetId }?.let {
            return it
        }
        
        // Check custom presets
        return getCustomPresets().find { it.id == presetId }
    }

    // Search presets by name
    fun searchPresets(query: String): Flow<List<FilterPreset>> = flow {
        val allPresets = getAllPresets()
        val results = allPresets.filter { 
            it.name.contains(query, ignoreCase = true) 
        }
        emit(results)
    }

    // Recent presets management
    fun addToRecent(presetId: String) {
        val recents = getRecentPresetIds().toMutableList()
        recents.remove(presetId)
        recents.add(0, presetId)
        
        // Keep only last 10
        while (recents.size > 10) {
            recents.removeAt(recents.size - 1)
        }
        
        saveRecentPresetIds(recents)
    }

    fun getRecentPresets(): Flow<List<FilterPreset>> = flow {
        val recentIds = getRecentPresetIds()
        val allPresets = getAllPresets()
        val recents = recentIds.mapNotNull { id ->
            allPresets.find { it.id == id }
        }
        emit(recents)
    }

    private fun getRecentPresetIds(): List<String> {
        val json = sharedPrefs.getString(recentPresetsKey, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveRecentPresetIds(ids: List<String>) {
        val json = gson.toJson(ids)
        sharedPrefs.edit().putString(recentPresetsKey, json).apply()
    }

    // Favorites management
    fun toggleFavorite(presetId: String): Boolean {
        val favorites = getFavoritePresetIds().toMutableSet()
        val isFavorite = presetId in favorites
        
        if (isFavorite) {
            favorites.remove(presetId)
        } else {
            favorites.add(presetId)
        }
        
        saveFavoritePresetIds(favorites)
        return !isFavorite
    }

    fun getFavoritePresets(): Flow<List<FilterPreset>> = flow {
        val favoriteIds = getFavoritePresetIds()
        val allPresets = getAllPresets()
        val favorites = allPresets.filter { it.id in favoriteIds }
        emit(favorites)
    }

    fun isFavorite(presetId: String): Boolean {
        return presetId in getFavoritePresetIds()
    }

    private fun getFavoritePresetIds(): Set<String> {
        val json = sharedPrefs.getString(favoritePresetsKey, null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(json, type) ?: emptySet()
    }

    private fun saveFavoritePresetIds(ids: Set<String>) {
        val json = gson.toJson(ids)
        sharedPrefs.edit().putString(favoritePresetsKey, json).apply()
    }

    // Create custom preset from current settings
    fun createCustomPreset(
        name: String,
        filterType: com.yourname.videoeditor.library.rendering.filter.FilterType,
        parameters: Map<String, Float>,
        intensity: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        hue: Float,
        sharpness: Float,
        blurRadius: Float,
        vignetteIntensity: Float,
        thumbnailPath: String? = null
    ): FilterPreset {
        return FilterPreset(
            name = name,
            filterType = filterType,
            parameters = parameters,
            intensity = intensity,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            hue = hue,
            sharpness = sharpness,
            blurRadius = blurRadius,
            vignetteIntensity = vignetteIntensity,
            thumbnailPath = thumbnailPath,
            category = PresetCategory.CUSTOM
        )
    }

    // Clear all custom presets
    fun clearAllCustomPresets() {
        sharedPrefs.edit().remove(customPresetsKey).apply()
    }
}