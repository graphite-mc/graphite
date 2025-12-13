package com.graphite.math.mat3

import com.graphite.math.floatArrayCopy

public fun Mat3.assign(other: Mat3) {
    floatArrayCopy(other.array, array, 0)
}
