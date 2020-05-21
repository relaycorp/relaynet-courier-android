package tech.relaycorp.courier.ui.settings

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.slider.Slider
import tech.relaycorp.courier.data.model.StorageSize

class StorageSlider
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Slider(context, attrs, defStyleAttr) {

    var sizeBoundary: SizeBoundary
        get() = SizeBoundary(
            min = StorageSize(valueFrom.toLong() * RESOLUTION),
            max = StorageSize(valueTo.toLong() * RESOLUTION),
            step = StorageSize(stepSize.toLong() * RESOLUTION)
        )
        set(value) {
            valueFrom = value.steppedMin.bytes.toFloat() / RESOLUTION
            valueTo = value.steppedMax.bytes.toFloat() / RESOLUTION
            stepSize = value.step.bytes.toFloat() / RESOLUTION
            resetSize()
        }

    var size: StorageSize
        get() = StorageSize(value.toLong() * RESOLUTION)
        set(newValue) {
            val newValueFloat = newValue.bytes / RESOLUTION
            // Make sure value is a multiple of the stepSize and between valueFrom and valueTo
            value = (newValueFloat - newValueFloat % stepSize).coerceIn(valueFrom, valueTo)
        }

    private fun resetSize() {
        size = StorageSize(value.toLong())
    }

    companion object {
        private const val RESOLUTION = 1_000_000
    }
}
