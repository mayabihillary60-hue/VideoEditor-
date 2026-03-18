package com.yourname.videoeditor.core.domain.repository

import com.yourname.videoeditor.core.domain.model.VideoProject
import kotlinx.coroutines.flow.Flow

interface VideoProjectRepository {
    fun getAllProjects(): Flow<List<VideoProject>>
    suspend fun getProjectById(id: String): VideoProject?
    suspend fun saveProject(project: VideoProject)
    suspend fun deleteProject(project: VideoProject)
    suspend fun updateProject(project: VideoProject)
}