import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

class FontArrayAdapter(context: Context, resource: Int, objects: List<String>, private val fontMap: Map<String, String>, private val customFontResourceMap: Map<String, Int>) : ArrayAdapter<String>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        setTypefaceForView(view, position)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        setTypefaceForView(view, position)
        return view
    }

    private fun setTypefaceForView(view: TextView, position: Int) {
        val fontName = getItem(position)
        val fontKey = fontMap[fontName]
        if (fontKey != null) {
            if (customFontResourceMap.containsKey(fontKey)) {
                val fontResourceId = customFontResourceMap[fontKey]
                if (fontResourceId != null) {
                    val customTypeface = ResourcesCompat.getFont(context, fontResourceId)
                    view.typeface = customTypeface
                }
            } else {
                view.typeface = Typeface.create(fontKey, Typeface.NORMAL)
            }
        }
    }
}
