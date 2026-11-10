package com.enes.movyswipe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes.movyswipe.R
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.databinding.ItemFilmCardBinding

class DuelloAdapter : RecyclerView.Adapter<DuelloAdapter.CardViewHolder>() {

    var filmListe: List<Movie> = emptyList()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder =
        CardViewHolder(ItemFilmCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val movie = filmListe[position]
        with(holder.binding) {
            tvFilmAdi.text = movie.displayTitle
            // Çıkış tarihi boş olan filmlerde eskiden substring() uygulamayı çökertiyordu.
            tvFilmYili.text = "📅 ${movie.releaseYear ?: "Bilinmiyor"}"
            tvFilmPuani.text = "⭐ ${MovieFormat.rating(movie.voteAverage)}/10"
            tvFilmOzet.text = movie.overview?.takeIf { it.isNotBlank() } ?: "Özet bulunmuyor."
            tvFilmTuru.text = "🎭 Film"
            Glide.with(root)
                .load(MovieFormat.posterUrl(movie.posterPath))
                .placeholder(R.drawable.placeholder_movie_poster)
                .error(R.drawable.placeholder_movie_poster)
                .into(ivFilmPoster)
        }
    }

    override fun getItemCount(): Int = filmListe.size

    fun listeyiGuncelle(yeniListe: List<Movie>) {
        if (yeniListe == filmListe) return
        filmListe = yeniListe
        @Suppress("NotifyDataSetChanged") // CardStackView desteyi baştan yükler
        notifyDataSetChanged()
    }

    class CardViewHolder(val binding: ItemFilmCardBinding) : RecyclerView.ViewHolder(binding.root)
}
