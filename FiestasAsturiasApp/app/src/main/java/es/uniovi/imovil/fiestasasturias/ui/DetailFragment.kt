package es.uniovi.imovil.fiestasasturias.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import es.uniovi.imovil.fiestasasturias.databinding.FragmentDetailBinding
import android.view.animation.DecelerateInterpolator
import es.uniovi.imovil.fiestasasturias.R

class DetailFragment : Fragment() {

    private lateinit var binding: FragmentDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // 🔙 BOTÓN VOLVER
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val nombre = arguments?.getString("nombre")
        val descripcion = arguments?.getString("descripcion")
        val localidad = arguments?.getString("localidad")
        val imagen = arguments?.getString("imagen")
        val email = arguments?.getString("email")
        val web = arguments?.getString("web")
        val dias = arguments?.getString("dias")
        val facebook = arguments?.getString("facebook")
        val instagram = arguments?.getString("instagram")
        val twitter = arguments?.getString("twitter")
        val youtube = arguments?.getString("youtube")
        val pinterest = arguments?.getString("pinterest")
        val rss = arguments?.getString("rss")
        val otrosCanales = arguments?.getStringArrayList("otrosCanales") ?: arrayListOf()

        binding.title.text = nombre
        binding.location.text = localidad
        binding.description.text = fromHtml(descripcion)

        val datesText = fromHtml(dias)
        binding.textDates.text = datesText
        binding.cardDates.visibility = if (datesText.isNullOrBlank()) View.GONE else View.VISIBLE

        val emailText = formatWithLabel(getString(R.string.detail_email), email)
        val webText = formatWithLabel(getString(R.string.detail_web), web)
        binding.textEmail.text = emailText
        binding.textWeb.text = webText
        binding.textEmail.visibility = if (emailText.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.textWeb.visibility = if (webText.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.cardContact.visibility = if (emailText.isNullOrBlank() && webText.isNullOrBlank()) View.GONE else View.VISIBLE

        bindEmail(email)
        bindUrl(web, binding.textWeb)

        val socialButtons = mutableListOf<Pair<String, String>>()
        addSocialButtonData(socialButtons, getString(R.string.detail_facebook), facebook)
        addSocialButtonData(socialButtons, getString(R.string.detail_instagram), instagram)
        addSocialButtonData(socialButtons, getString(R.string.detail_twitter), twitter)
        addSocialButtonData(socialButtons, getString(R.string.detail_youtube), youtube)
        addSocialButtonData(socialButtons, getString(R.string.detail_pinterest), pinterest)
        addSocialButtonData(socialButtons, getString(R.string.detail_rss), rss)
        socialButtons.addAll(parseOtherChannels(otrosCanales))

        renderSocialButtons(socialButtons)
        binding.cardSocial.visibility = if (socialButtons.isEmpty()) View.GONE else View.VISIBLE

        Glide.with(requireContext())
            .load(imagen)
            .placeholder(R.drawable.ic_launcher_background)
            .centerCrop()
            .into(binding.image)

        // ✨ ANIMACIÓN DE ENTRADA (PRO)
        binding.root.apply {
            alpha = 0f
            scaleX = 0.95f
            scaleY = 0.95f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun bindEmail(email: String?) {
        val cleanEmail = email?.trim()?.takeIf { it.isNotEmpty() } ?: return
        binding.textEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$cleanEmail")
            }
            startActivity(intent)
        }
    }

    private fun bindUrl(url: String?, textView: TextView) {
        val cleanUrl = url?.trim()?.takeIf { it.isNotEmpty() } ?: return
        textView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
            startActivity(intent)
        }
    }

    private fun addSocialButtonData(lines: MutableList<Pair<String, String>>, label: String, url: String?) {
        val clean = url?.trim()?.takeIf { it.isNotEmpty() } ?: return
        lines.add(label to clean)
    }

    private fun formatWithLabel(label: String, value: String?): String? {
        val clean = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return "$label: $clean"
    }

    private fun fromHtml(value: String?): CharSequence? {
        val clean = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return Html.fromHtml(clean, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun parseOtherChannels(rawChannels: List<String>): List<Pair<String, String>> {
        return rawChannels.mapNotNull { item ->
            val parts = item.split(": ", limit = 2)
            val label = parts.getOrNull(0)?.trim().orEmpty()
            val url = parts.getOrNull(1)?.trim().orEmpty()
            if (label.isEmpty() || url.isEmpty()) null else label to url
        }
    }

    private fun renderSocialButtons(channels: List<Pair<String, String>>) {
        binding.socialButtonsContainer.removeAllViews()
        channels.forEach { (label, url) ->
            val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                isAllCaps = false
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
                val horizontal = dpToPx(14)
                val vertical = dpToPx(10)
                setPadding(horizontal, vertical, horizontal, vertical)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            binding.socialButtonsContainer.addView(button, params)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
