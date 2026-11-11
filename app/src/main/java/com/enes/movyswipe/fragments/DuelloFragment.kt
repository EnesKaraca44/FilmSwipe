package com.enes.movyswipe.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.enes.movyswipe.util.UserStatsManager
import com.enes.movyswipe.viewmodels.DuelloViewModel
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DuelloFragment : Fragment(), CardStackListener {

    private var _binding: FragmentDuelloBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DuelloViewModel by viewModels()
    private val args: DuelloFragmentArgs by navArgs()

    private lateinit var cardStackAdapter: DuelloAdapter
    private lateinit var layoutManager: CardStackLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDuelloBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardStackView()
        // ViewModel bunu yalnızca ilk seferde işler (ekran döndürmede deste baştan yüklenmez).
        viewModel.start(args.roomCode)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupCardStackView() {
        layoutManager = CardStackLayoutManager(requireContext(), this)
        layoutManager.setCanScrollVertical(false)
        cardStackAdapter = DuelloAdapter()
        binding.cardStackView.layoutManager = layoutManager
        binding.cardStackView.adapter = cardStackAdapter
    }

    private fun setupClickListeners() {
        binding.btnLike.setOnClickListener { swipe(Direction.Right) }
        binding.btnDislike.setOnClickListener { swipe(Direction.Left) }
        binding.errorView.btnRetry.setOnClickListener { viewModel.retry() }
    }

    private fun swipe(direction: Direction) {
        if (layoutManager.topPosition >= cardStackAdapter.itemCount) return
        layoutManager.setSwipeAnimationSetting(SwipeAnimationSetting.Builder().setDirection(direction).build())
        binding.cardStackView.swipe()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.movies.collect { movieList ->
                        if (movieList.isNotEmpty()) binding.contentLayout.isVisible = true
                        cardStackAdapter.listeyiGuncelle(movieList)
                        val resumeAt = viewModel.swipedCount
                        if (resumeAt in 1..movieList.size && layoutManager.topPosition != resumeAt) {
                            binding.cardStackView.scrollToPosition(resumeAt)
                        }
                    }
                }
                launch {
                    viewModel.genreName.collect { genreName ->
                        binding.tvGenreName.isVisible = genreName != null
                        if (genreName != null) binding.tvGenreName.text = getString(R.string.duel_genre_label, genreName)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        if (isLoading) binding.contentLayout.isVisible = false
                    }
                }
                launch {
                    // Hata, toast yerine kalıcı panelde gösterilir: ekran döndürmede/geri gelmede
                    // aynı toast tekrar çıkmaz ve kullanıcı boş ekranda kalmaz.
                    viewModel.error.collect { error ->
                        binding.errorView.root.isVisible = error != null
                        if (error != null) {
                            binding.errorView.tvErrorMessage.text = error
                            binding.progressBar.isVisible = false
                            binding.contentLayout.isVisible = false
                        }
                    }
                }
                launch {
                    combine(viewModel.waitingForPartner, viewModel.partnerOffline, ::Pair)
                        .collect { (waiting, offline) ->
                            binding.tvPartnerStatus.setText(
                                when {
                                    offline -> R.string.duel_partner_offline
                                    waiting -> R.string.duel_partner_waiting
                                    else -> R.string.duel_partner_ready
                                }
                            )
                            binding.btnLike.isEnabled = !waiting
                            binding.btnDislike.isEnabled = !waiting
                        }
                }
                launch { viewModel.partnerLeft.collect { left -> if (left) showPartnerLeftDialog() } }
                launch { viewModel.matchState.collect(::handleMatchState) }
            }
        }
    }

    private var partnerLeftDialog: AlertDialog? = null

    private fun showPartnerLeftDialog() {
        if (partnerLeftDialog?.isShowing == true) return
        partnerLeftDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.duel_partner_left_title)
            .setMessage(R.string.duel_partner_left_message)
            .setCancelable(false)
            .setPositiveButton(R.string.duel_partner_left_button) { _, _ ->
                val nav = findNavController()
                if (!nav.popBackStack(R.id.anaMenuFragment, false)) nav.navigateUp()
            }
            .show()
    }

    private fun handleMatchState(state: MatchState) {
        val nav = findNavController()
        if (nav.currentDestination?.id != R.id.duelloFragment) return
        when (state) {
            is MatchState.MATCH_FOUND -> {
                UserStatsManager.recordDuel(requireContext().applicationContext, args.roomCode, isMatch = true)
                nav.navigate(
                    DuelloFragmentDirections.actionDuelloFragmentToEslemeFragment(
                        matchedMovieId = state.movieId,
                        roomCode = args.roomCode
                    )
                )
            }
            MatchState.NO_MATCH -> {
                UserStatsManager.recordDuel(requireContext().applicationContext, args.roomCode, isMatch = false)
                nav.navigate(
                    DuelloFragmentDirections.actionDuelloFragmentToEslemeYokFragment(checkedCount = cardStackAdapter.itemCount)
                )
            }
            MatchState.IDLE -> Unit
        }
    }

    override fun onCardSwiped(direction: Direction?) {
        val swipedPosition = layoutManager.topPosition - 1
        val currentList = cardStackAdapter.filmListe
        if (swipedPosition in currentList.indices) {
            viewModel.submitChoice(currentList[swipedPosition].id, liked = direction == Direction.Right)
        }
        if (currentList.isNotEmpty() && layoutManager.topPosition == cardStackAdapter.itemCount) {
            viewModel.onDeckFinished()
        }
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        partnerLeftDialog?.dismiss()
        partnerLeftDialog = null
        _binding = null
    }
}
