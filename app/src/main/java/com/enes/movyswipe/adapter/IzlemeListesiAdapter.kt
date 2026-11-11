package com.enes.movyswipe.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.enes.movyswipe.R
import com.enes.movyswipe.data.MovieFormat
import com.enes.movyswipe.data.RelativeDay
import com.enes.movyswipe.data.WatchlistMovie
import com.enes.movyswipe.data.WatchlistSort
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
            val context = binding.root.context
            binding.tvEklemeTarihi.text = context.getString(
                R.string.watchlist_added_ago,
                context.relativeDayLabel(WatchlistStats.relativeDay(daysAgo.toInt()))
            )
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

/** "Bugün", "Dün", "3 gün önce"... (Listem ekranı; metinler string kaynaklarından). */
internal fun Context.relativeDayLabel(day: RelativeDay): String = when (day.kind) {
    RelativeDay.Kind.TODAY -> getString(R.string.watchlist_relative_today)
    RelativeDay.Kind.YESTERDAY -> getString(R.string.watchlist_relative_yesterday)
    RelativeDay.Kind.DAYS -> resources.getQuantityString(R.plurals.watchlist_relative_days_ago, day.amount, day.amount)
    RelativeDay.Kind.WEEKS -> resources.getQuantityString(R.plurals.watchlist_relative_weeks_ago, day.amount, day.amount)
    RelativeDay.Kind.MONTHS -> resources.getQuantityString(R.plurals.watchlist_relative_months_ago, day.amount, day.amount)
    RelativeDay.Kind.YEARS -> resources.getQuantityString(R.plurals.watchlist_relative_years_ago, day.amount, day.amount)
}

/** Sıralama seçeneğinin görünen adı. */
@get:StringRes
internal val WatchlistSort.labelRes: Int
    get() = when (this) {
        WatchlistSort.NEWEST -> R.string.watchlist_sort_newest
        WatchlistSort.OLDEST -> R.string.watchlist_sort_oldest
        WatchlistSort.TITLE -> R.string.watchlist_sort_title
        WatchlistSort.RATING -> R.string.watchlist_sort_rating
        WatchlistSort.RELEASE -> R.string.watchlist_sort_release
    }
