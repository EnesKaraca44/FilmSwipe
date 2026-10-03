package com.enes.movyswipe.fragments


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.enes.movyswipe.R
import com.enes.movyswipe.adapter.DuelloAdapter
import com.enes.movyswipe.data.repository.MatchState
import com.enes.movyswipe.databinding.FragmentDuelloBinding
import com.enes.movyswipe.util.AuthManager
import com.enes.movyswipe.viewmodels.DuelloViewModel

import kotlinx.coroutines.launch
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DuelloFragment : Fragment(),CardStackListener {
    private  var _binding: FragmentDuelloBinding?=null
    private val binding get() = _binding!!

    private val viewModel: DuelloViewModel by viewModels()

    private val args: DuelloFragmentArgs by navArgs()

    private lateinit var cardStackAdapter: DuelloAdapter
    private lateinit var layoutManager: CardStackLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentDuelloBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCardStackView()
        viewModel.loadMovieDeck(args.roomCode)
        viewModel.startListeningForMatch(args.roomCode)
        setupClickListeners()
        observeViewModel() // Gözlemci fonksiyonumuzu çağırıyoruz.
    }
    // Bu fonksiyon, CardStackView'ı ayarlar.
    private fun setupCardStackView() {
        // 6. Layout Manager'ı oluşturuyoruz. 'this' diyerek, bu Fragment'ın
        // CardStackListener olduğunu belirtiyoruz.
        layoutManager = CardStackLayoutManager(requireContext(), this)
        layoutManager.setCanScrollVertical(false) // Dikey kaydırmayı kapatıyoruz.

        // 7. Adaptörü oluşturuyoruz. Başlangıçta boş bir liste veriyoruz.
        // `requireContext()` bu Fragment'ın bağlı olduğu Context'i güvenli bir şekilde verir.
        cardStackAdapter = DuelloAdapter(requireContext(), emptyList())

        // 8. Ayarladığımız Layout Manager ve Adaptörü CardStackView'a bağlıyoruz.
        binding.cardStackView.layoutManager = layoutManager
        binding.cardStackView.adapter = cardStackAdapter
    }
    // Bu fonksiyon, butonların tıklama olaylarını ayarlar.
    private fun setupClickListeners() {
        binding.btnLike.setOnClickListener {
            // Beğen butonuna basıldığında kartı sağa kaydır.
            layoutManager.setSwipeAnimationSetting(com.yuyakaido.android.cardstackview.SwipeAnimationSetting.Builder().setDirection(Direction.Right).build())
            binding.cardStackView.swipe()
        }

        binding.btnDislike.setOnClickListener {
            // Beğenmeme butonuna basıldığında kartı sola kaydır.
            layoutManager.setSwipeAnimationSetting(com.yuyakaido.android.cardstackview.SwipeAnimationSetting.Builder().setDirection(Direction.Left).build())
            binding.cardStackView.swipe()
        }
    }


    // Bu fonksiyon, ViewModel'deki değişiklikleri dinler.
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Film listesini dinle (Bu zaten vardı)
                launch {
                    viewModel.movies.collect { movieList ->
                        if (movieList.isNotEmpty()) {
                            binding.contentLayout.isVisible = true // Filmler gelince içeriği göster
                        }
                        cardStackAdapter.listeyiGuncelle(movieList)
                    }
                }
                launch {
                    viewModel.genreName.collect { genreName ->
                        if (genreName != null) {
                            // Tür adı geldiyse, TextView'a yazdır.
                            binding.tvGenreName.text = "Tür: $genreName"
                            binding.tvGenreName.isVisible = true
                        } else {
                            // Tür adı yoksa (örneğin normal oda kurma), TextView'ı gizle.
                            binding.tvGenreName.isVisible = false
                        }
                    }
                }

                // --- YENİ EKLENEN KISIM 1: Yükleme Durumunu Dinle ---
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        // Eğer yükleniyorsa, film kartlarını gösterme
                        if (isLoading) {
                            binding.contentLayout.isVisible = false
                        }
                    }
                }

                // --- YENİ EKLENEN KISIM 2: Hata Durumunu Dinle ---
                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                            // Hata olduğunda yüklemeyi durdur ve progress bar'ı gizle
                            binding.progressBar.isVisible = false
                        }
                    }
                }

                // Eşleşme durumu dinleyicisi (Bu da Gün 9'dan vardı)
                launch {
                    viewModel.matchState.collect { state ->
                        Log.d("DuelloFragment", "Yeni MatchState geldi: $state")
                        when (state) {
                            is MatchState.MATCH_FOUND -> {
                                // EŞLEŞME BULUNDUĞUNDA:
                                // Eşleşme ekranına yönlendir.
                                if (findNavController().currentDestination?.id == R.id.duelloFragment) {
                                    val action =
                                        DuelloFragmentDirections.actionDuelloFragmentToEslemeFragment(
                                            matchedMovieId = state.movieId,
                                            roomCode = args.roomCode
                                        )
                                    withContext(Dispatchers.Main) {
                                        findNavController().navigate(action)
                                    }
                                }
                            }

                            is MatchState.NO_MATCH -> {
                                // EŞLEŞME BULUNAMADIĞINDA:
                                // Eşleşme yok ekranına yönlendir.
                                if (findNavController().currentDestination?.id == R.id.duelloFragment) {
                                    val action =
                                        DuelloFragmentDirections.actionDuelloFragmentToEslemeYokFragment()
                                    findNavController().navigate(action)
                                }
                            }

                            is MatchState.IDLE -> {
                                // BAŞLANGIÇ DURUMU:
                                // Bir şey yapmamıza gerek yok, sadece bekliyoruz.
                            }
                            // 'else' bloğuna gerek yok çünkü 'sealed class' ile tüm durumları
                            // ele aldık.
                        }
                    }

                }
            }
        }
    }

    override fun onCardSwiped(direction: Direction?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val swipedCardPosition = layoutManager.topPosition - 1
            val currentList = cardStackAdapter.filmListe

            if (swipedCardPosition >= 0 && swipedCardPosition < currentList.size) {
                val swipedMovie = currentList[swipedCardPosition]
                val choice = if (direction == Direction.Right) "like" else "pass"
                val userId = AuthManager.getCurrentUserId()

                // ViewModel'i tekrar devreye sokuyoruz.
                viewModel.submitChoice(args.roomCode, swipedMovie.id, choice, userId)
            }

            if (layoutManager.topPosition == cardStackAdapter.itemCount) {
                viewModel.onDeckFinished()
            }
        }
    }

    // Diğer listener metotları. Şimdilik boş kalabilirler.
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


