package tech.relaycorp.courier.ui.settings

import android.widget.SeekBar
import tech.relaycorp.courier.data.model.StorageSize

class StorageSizeSeekBarHelper(
    unadjustedBoundary: Boundary<StorageSize>,
    seekBar: SeekBar
) : SeekBar.OnSeekBarChangeListener {

    var onChangeListener: ((StorageSize) -> Unit)? = null

    private val boundary =
        // Ensure boundary is integer, and min and max are aligned with the step
        unadjustedBoundary
            .map { it.bytes / MAX_RESOLUTION }
            .let {
                Boundary(
                    it.min - (it.min % it.step),
                    it.max - (it.max % it.step),
                    it.step
                )
            }

    init {
        seekBar.max = ((boundary.max / boundary.step) - (boundary.min / boundary.step)).toInt()
        seekBar.setOnSeekBarChangeListener(this)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        onChangeListener?.invoke(
            StorageSize((boundary.step * progress + boundary.min) * MAX_RESOLUTION)
        )
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit

    companion object {
        private const val MAX_RESOLUTION = 1_000_000L
    }
}
