package com.enes.movyswipe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes.movyswipe.R
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.data.WatchlistStats
import com.enes.movyswipe.databinding.ItemIzlemeListesiFilmBinding

class IzlemeListesiAdapter : ListAdapter<WatchlistMovie, IzlemeListesiAdapter.WatchlistViewHolder>(DiffCallback()) {

    var onDeleteClick: (WatchlistMovie) -> Unit = {}

    inner class WatchlistViewHolder(private val binding: ItemIzlemeListesiFilmBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: WatchlistMovie) {
            binding.tvFilmAdi.text = movie.title
            binding.tvFilmYili.text = MovieFormat.releaseYear(movie.releaseDate) ?: "—"
            binding.tvFilmPuani.text = MovieFormat.rating(movie.voteAverage)
            val daysAgo = WatchlistStats.epochDay(System.currentTimeMillis()) - WatchlistStats.epochDay(movie.addedTimestamp)
            binding.tvEklemeTarihi.text = "Eklendi • ${WatchlistStats.relativeDayLabel(daysAgo.toInt())}"
            Glide.with(binding.root)
                .load(MovieFormat.posterUrl(movie.posterPath))
                .placeholder(R.drawable.placeholder_movie_poster)
                .error(R.drawable.placeholder_movie_poster)
                .into(binding.imgFilmPoster)
            binding.btnKaldir.setOnClickListener { onDeleteClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchlistViewHolder =
        WatchlistViewHolder(ItemIzlemeListesiFilmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: WatchlistViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<WatchlistMovie>() {
        override fun areItemsTheSame(oldItem: WatchlistMovie, newItem: WatchlistMovie) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WatchlistMovie, newItem: WatchlistMovie) = oldItem == newItem
    }
}
