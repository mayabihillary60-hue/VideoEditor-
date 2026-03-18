package com.yourname.videoeditor.core.domain.usecase

import com.yourname.videoeditor.core.domain.model.VideoProject
import com.yourname.videoeditor.core.domain.repository.VideoProjectRepository

class CreateNewProjectUseCase(
    private val repository: VideoProjectRepository
) {
    suspend operator fun invoke(name: String): VideoProject {
        val project = VideoProject(name = name)
        repository.saveProject(project)
        return project
    }
}