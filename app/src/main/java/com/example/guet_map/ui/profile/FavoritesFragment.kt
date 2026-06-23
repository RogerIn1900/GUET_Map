package com.example.guet_map.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentFavoritesBinding
import com.example.guet_map.module.social.data.model.Favorite
import com.example.guet_map.module.social.data.model.FavoriteCategory
import com.example.guet_map.module.social.ui.favorites.FavoriteAdapter
import com.example.guet_map.module.social.ui.favorites.FavoritesUiState
import com.example.guet_map.module.social.ui.favorites.FavoritesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()

    private lateinit var adapter: FavoriteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupChips()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = FavoriteAdapter(
            onItemClick = { favorite ->
                // 点击收藏项，跳转到地图
                val bundle = Bundle().apply {
                    putString("locationId", favorite.locationId)
                }
                findNavController().navigate(R.id.nav_map, bundle)
            },
            onDeleteClick = { favorite ->
                viewModel.removeFavorite(favorite)
            }
        )
        binding.rvFavorites.layoutManager = LinearLayoutManager(context)
        binding.rvFavorites.adapter = adapter
    }

    private fun setupChips() {
        binding.chipAll.setOnClickListener {
            viewModel.selectCategory(FavoriteCategory.ALL)
        }
        binding.chipClassroom.setOnClickListener {
            viewModel.selectCategory(FavoriteCategory.STUDY)
        }
        binding.chipCanteen.setOnClickListener {
            viewModel.selectCategory(FavoriteCategory.FOOD)
        }
        binding.chipLibrary.setOnClickListener {
            viewModel.selectCategory(FavoriteCategory.STUDY)
        }
        binding.chipDorm.setOnClickListener {
            viewModel.selectCategory(FavoriteCategory.DAILY)
        }
        binding.chipGate.setOnClickListener {
            viewModel.selectCategory(FavoriteCategory.OTHER)
        }
        binding.chipAll.isChecked = true
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favorites.collect { favorites ->
                    updateList(favorites)
                }
            }
        }
    }

    private fun render(state: FavoritesUiState) {
        when (state) {
            is FavoritesUiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.rvFavorites.isVisible = false
                binding.layoutEmpty.isVisible = false
            }
            is FavoritesUiState.Empty -> {
                binding.progressBar.isVisible = false
                binding.rvFavorites.isVisible = false
                binding.layoutEmpty.isVisible = true
            }
            is FavoritesUiState.Success -> {
                binding.progressBar.isVisible = false
                binding.layoutEmpty.isVisible = false
            }
        }
    }

    private fun updateList(favorites: List<Favorite>) {
        adapter.submitList(favorites)
        binding.rvFavorites.isVisible = favorites.isNotEmpty()
        binding.layoutEmpty.isVisible = favorites.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
