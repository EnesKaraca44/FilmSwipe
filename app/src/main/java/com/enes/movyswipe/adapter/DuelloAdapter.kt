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
            val ctx = root.context
            tvFilmYili.text = ctx.getString(R.string.duel_card_year, movie.releaseYear ?: ctx.getString(R.string.duel_unknown_year))
            tvFilmPuani.text = ctx.getString(R.string.duel_card_rating, MovieFormat.rating(movie.voteAverage))
            tvFilmOzet.text = movie.overview?.takeIf { it.isNotBlank() } ?: ctx.getString(R.string.duel_no_overview)
            tvFilmTuru.setText(R.string.duel_card_genre_placeholder)
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
