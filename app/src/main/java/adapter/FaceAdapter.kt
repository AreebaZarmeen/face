
package adapters

import android.graphics.BitmapFactory
import android.hardware.camera2.params.Face
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//import com.example.face
import com.example.face.R
import database.face

class FaceAdapter(
    private var faces: List<face>,
    private val onDeleteClick: (face) -> Unit
) : RecyclerView.Adapter<FaceAdapter.FaceViewHolder>() {

    class FaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textView: TextView = view.findViewById(R.id.textView)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_face, parent, false)
        return FaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaceViewHolder, position: Int) {
        val face = faces[position]
        holder.imageView.setImageBitmap(BitmapFactory.decodeFile(face.imagePath))
        holder.textView.text = face.name
        holder.deleteButton.setOnClickListener { onDeleteClick(face) }
    }

    override fun getItemCount() = faces.size

    fun updateData(newFaces: List<face>) {
        faces = newFaces
        notifyDataSetChanged()
    }
}
