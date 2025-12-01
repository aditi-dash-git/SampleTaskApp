package com.example.sampletaskapp

//package com.example.sampletaskapp

enum class TaskType {
    TEXT_READING,
    IMAGE_DESCRIPTION,
    PHOTO_CAPTURE
}

data class Task(
    val id: Long,
    val taskType: TaskType,
    val text: String? = null,
    val imageUrl: String? = null,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val durationSec: Int,
    val timestamp: String
)
