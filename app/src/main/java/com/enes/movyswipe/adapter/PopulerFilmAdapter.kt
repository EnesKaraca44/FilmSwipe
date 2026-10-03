package com.enes.movyswipe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.databinding.ItemPopulerFilmBinding // XML adına göre bu değişebilir

// 'onItemClick' parametresi, her bir filme tıklandığında ne olacağını Fragment'tan almamızı sağlar.
class PopulerFilmAdapter(private val onItemClick: (Movie) -> Unit) :
    ListAdapter<Movie, PopulerFilmAdapter.MovieViewHolder>(DiffCallback()) {

    inner class MovieViewHolder(private val binding: ItemPopulerFilmBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
            Glide.with(binding.root.context)
                .load(posterUrl)
                .into(binding.imageViewFilmPoster) // XML'deki ImageView'ın ID'si

            // Postere tıklandığında, constructor'dan gelen 'onItemClick' fonksiyonunu çağır.
            binding.root.setOnClickListener {
                onItemClick(movie)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemPopulerFilmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}