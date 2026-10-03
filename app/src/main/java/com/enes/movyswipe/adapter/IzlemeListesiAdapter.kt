package com.enes.movyswipe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.databinding.ItemIzlemeListesiFilmBinding // XML adına göre bu değişebilir

class IzlemeListesiAdapter : ListAdapter<WatchlistMovie, IzlemeListesiAdapter.WatchlistViewHolder>(DiffCallback()) {


    // Bu, dışarıdan (Fragment'tan) doldurulacak olan bir "tıklama dinleyici" fonksiyonudur.
    // Başlangıçta boş bir fonksiyon olarak tanımlıyoruz.
    var onDeleteClick: (WatchlistMovie) -> Unit = {}

    inner class WatchlistViewHolder(private val binding: ItemIzlemeListesiFilmBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: WatchlistMovie) {
            binding.tvFilmAdi.text = movie.title
            binding.tvFilmYili.text = movie.releaseDate.substring(0, 4)

            val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
            Glide.with(binding.root.context)
                .load(posterUrl)
                .into(binding.imgFilmPoster)

            binding.btnKaldir.setOnClickListener {
                // Tıklandığında, yukarıda tanımladığımız 'onDeleteClick' fonksiyonunu çağırıyoruz.
                onDeleteClick(movie)
            }

            // TODO: Diğer alanları (tür, süre, ekleme tarihi) doldurma mantığı eklenebilir.
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchlistViewHolder {
        val binding = ItemIzlemeListesiFilmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WatchlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<WatchlistMovie>() {
        override fun areItemsTheSame(oldItem: WatchlistMovie, newItem: WatchlistMovie): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: WatchlistMovie, newItem: WatchlistMovie): Boolean {
            return oldItem == newItem
        }
    }
}