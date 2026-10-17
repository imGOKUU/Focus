package com.meltide.app.data.local.entity

// Mirrors the lifecycle in spec section 7:
// PENDING -> STARTED -> COMPLETED -> FINISHED
//                     -> INTERRUPTED -> RESUME / END
enum class SessionStatus { STARTED, COMPLETED,PAUSED, INTERRUPTED, FINISHED }
