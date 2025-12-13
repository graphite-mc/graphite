package com.graphite.math.mat4

import com.graphite.math.floatArrayCopy

public fun Mat4.assign(other: Mat4) {
    floatArrayCopy(other.array, array, 0)
}
