package com.enes.movyswipe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes.movyswipe.R
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.databinding.ItemPopulerFilmBinding

class PopulerFilmAdapter(private val onItemClick: (Movie) -> Unit) :
    ListAdapter<Movie, PopulerFilmAdapter.MovieViewHolder>(DiffCallback()) {

    inner class MovieViewHolder(private val binding: ItemPopulerFilmBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.imageViewFilmPoster.contentDescription = movie.displayTitle
            // Eskiden layout'taki sabit "Movie Name / 8.5" metni her filmde görünüyordu.
            binding.textViewFilmTitle.text = movie.displayTitle
            binding.textViewFilmRating.text = MovieFormat.rating(movie.voteAverage)
            Glide.with(binding.root)
                .load(MovieFormat.posterUrl(movie.posterPath))
                .placeholder(R.drawable.placeholder_movie_poster)
                .error(R.drawable.placeholder_movie_poster)
                .into(binding.imageViewFilmPoster)
            binding.root.setOnClickListener { onItemClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder =
        MovieViewHolder(ItemPopulerFilmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
