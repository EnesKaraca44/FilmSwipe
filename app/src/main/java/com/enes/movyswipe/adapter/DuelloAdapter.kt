package com.enes.movyswipe.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.databinding.ItemFilmCardBinding



class DuelloAdapter(var Mcontex:Context,var filmListe: List<Movie>) : RecyclerView.Adapter<DuelloAdapter.CardTasarimTutucu>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CardTasarimTutucu {
        var binding= ItemFilmCardBinding.inflate(LayoutInflater.from(Mcontex), parent, false)
        return CardTasarimTutucu(binding)
    }

    override fun onBindViewHolder(
        holder: CardTasarimTutucu,
        position: Int
    ) {
        var tasarim=filmListe.get(position)
        var t=holder.tasarim
        t.tvFilmAdi.text = tasarim.title
        t.tvFilmYili.text = "📅 ${tasarim.releaseDate.substring(0, 4)}" // Sadece yılı alıyoruz
        t.tvFilmPuani.text = "⭐ ${tasarim.voteAverage}/10"
        t.tvFilmOzet.text = tasarim.overview

        // Glide ile posteri yüklüyoruz
        // TMDB'den gelen poster yolu tam bir URL değildir, onu biz birleştiririz.
        val posterUrl = "https://image.tmdb.org/t/p/w500${tasarim.posterPath}"
        Glide.with(Mcontex)
            .load(posterUrl)
            .into(t.ivFilmPoster)

        // Film türü gibi alanları daha sonra ekleyebiliriz
        t.tvFilmTuru.text = "🎭 Film"

    }

    override fun getItemCount(): Int {
       return filmListe.size
    }

    class DiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
    fun listeyiGuncelle(yeniListe: List<Movie>) {
        filmListe = yeniListe // Adaptörün kendi listesini yeni gelen listeyle değiştiriyoruz.
        notifyDataSetChanged() // Bu komut, RecyclerView'a "Tüm listeyi baştan çiz" der.
    }

    inner class CardTasarimTutucu(var tasarim:ItemFilmCardBinding) :
        RecyclerView.ViewHolder(tasarim.root) {
    }
}