package ru.netology.fmhandroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.netology.fmhandroid.R
import ru.netology.fmhandroid.adapter.ClaimCommentListAdapter
import ru.netology.fmhandroid.adapter.OnCommentItemClickListener
import ru.netology.fmhandroid.databinding.FragmentOpenClaimBinding
import ru.netology.fmhandroid.dto.*
import ru.netology.fmhandroid.utils.Events
import ru.netology.fmhandroid.utils.Utils
import ru.netology.fmhandroid.viewmodel.ClaimViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@AndroidEntryPoint
class OpenClaimFragment : Fragment() {
    private lateinit var binding: FragmentOpenClaimBinding
    private val viewModel: ClaimViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_open_claim, container, false)
    }

    //    TODO("В этом фрагменте после внедрения авторизации требуется изменить хардкод юзера на залогиненного пользователя!!!!")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOpenClaimBinding.bind(view)

        val args: OpenClaimFragmentArgs by navArgs()
        val claim = args.argClaim

        // Временная переменная. После авторизации заменить на залогиненного юзера
        val user = User(
            id = 1,
            login = "User-1",
            password = "abcd",
            firstName = "Дмитрий",
            lastName = "Винокуров",
            middleName = "Владимирович",
            phoneNumber = "+79109008765",
            email = "Vinokurov@mail.ru",
            deleted = false
        )

        val adapter = ClaimCommentListAdapter(object : OnCommentItemClickListener {
            override fun onCard(claimComment: ClaimCommentWithCreator) {
                val action = OpenClaimFragmentDirections
                    .actionOpenClaimFragmentToCreateEditClaimCommentFragment(
                        claimComment,
                        claim.claim.id!!
                    )
                findNavController().navigate(action)
            }
        })

        val statusProcessingMenu = PopupMenu(context, binding.statusProcessingImageButton)
        statusProcessingMenu.inflate(R.menu.menu_status_processing)

        if (claim.claim.creatorId != user.id) {
            statusProcessingMenu.menu.removeItem(R.id.cancel_list_item)
        }
        statusMenuVisibility(claim.claim.status!!, statusProcessingMenu)
        statusProcessingMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.take_to_work_list_item -> {

                    lifecycleScope.launchWhenStarted {
                        //Изменить на залогиненного юзера!
                        viewModel.updateClaim(updatedClaim = claim.claim.copy(executorId = user.id))

                        Events.events.collect { event ->
                            when (event) {
                                viewModel.claimUpdateExceptionEvent -> {
                                    showErrorToast()
                                    return@collect
                                }

                                viewModel.claimStatusChangeExceptionEvent -> {
                                    showErrorToast()
                                    return@collect
                                }

                                viewModel.claimUpdatedEvent -> {
                                    viewModel.changeClaimStatus(
                                        claim.claim.id!!,
                                        Claim.Status.IN_PROGRESS
                                    )
                                }
                                viewModel.claimStatusChangedEvent -> {
                                    binding.executorNameTextView.text =
                                        Utils.fullUserNameGenerator(
                                            user.lastName.toString(),
                                            user.firstName.toString(),
                                            user.middleName.toString()
                                        )

                                    viewModel.dataClaim.collect {
                                        binding.statusLabelTextView.text =
                                            displayingStatusOfClaim(it.claim.status!!)
                                        statusMenuVisibility(
                                            it.claim.status!!,
                                            statusProcessingMenu
                                        )
                                    }
                                }
                            }
                        }
                    }
                    true
                }

                R.id.cancel_list_item -> {

                    viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                        viewModel.changeClaimStatus(claim.claim.id!!, Claim.Status.CANCELLED)
                        Events.events.collect {
                            viewModel.claimStatusChangedEvent
                            viewModel.dataClaim.collect {
                                binding.statusLabelTextView.text =
                                    displayingStatusOfClaim(it.claim.status!!)
                                statusMenuVisibility(it.claim.status!!, statusProcessingMenu)
                            }
                        }
                    }
                    true
                }

                R.id.throw_off_list_item -> {

                    viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                        createClaimCommentDialog(claim, user)
                        viewModel.updateClaim(claim.claim.copy(executorId = null))
                        viewModel.changeClaimStatus(claim.claim.id!!, Claim.Status.OPEN)

                        Events.events.collect { event ->
                            when (event) {
                                viewModel.claimCommentCreateExceptionEvent -> {
                                    showErrorToast()
                                    return@collect
                                }
                                viewModel.claimUpdateExceptionEvent -> {
                                    showErrorToast()
                                    return@collect
                                }
                                viewModel.claimStatusChangeExceptionEvent -> {
                                    showErrorToast()
                                    return@collect
                                }
                            }

                            binding.executorNameTextView.setText(R.string.not_assigned)
                            viewModel.dataClaim.collect {
                                binding.statusLabelTextView.text =
                                    displayingStatusOfClaim(it.claim.status!!)
                                statusMenuVisibility(it.claim.status!!, statusProcessingMenu)
                            }
                        }
                    }
                    true
                }

                R.id.executes_list_item -> {

                    viewLifecycleOwner.lifecycleScope.launch {
                        createClaimCommentDialog(claim, user)

                        Events.events.collect { event ->
                            when (event) {
                                viewModel.claimCommentCreatedEvent -> {
                                    viewModel.updateClaim(
                                        updatedClaim = claim.claim.copy(
                                            factExecuteDate = LocalDateTime.now().toEpochSecond(
                                                ZoneId.of("Europe/Moscow").rules.getOffset(
                                                    Instant.now()
                                                )
                                            )
                                        )
                                    )
                                }
                                viewModel.claimUpdatedEvent -> {
                                    viewModel.changeClaimStatus(
                                        claim.claim.id!!,
                                        Claim.Status.EXECUTED
                                    )
                                }
                            }
                        }
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        Events.events.collect {
                            viewModel.claimStatusChangedEvent
                            viewModel.dataClaim.collect {
                                binding.statusLabelTextView.text =
                                    displayingStatusOfClaim(it.claim.status!!)
                                statusMenuVisibility(it.claim.status!!, statusProcessingMenu)
                            }
                        }
                    }

                    true
                }
                else -> {
                    false
                }
            }
        }

        binding.apply {

            if (claim.claim.status == Claim.Status.CANCELLED || claim.claim.status == Claim.Status.EXECUTED) {
                statusProcessingImageButton.visibility = View.INVISIBLE
                editProcessingImageButton.visibility = View.INVISIBLE
            }

            if (claim.claim.status == Claim.Status.IN_PROGRESS) {
                editProcessingImageButton.visibility = View.INVISIBLE
            }

            // Изменить на залогиненного юзера и добавить в условие администратора
            if (claim.claim.executorId != user.id && claim.claim.status == Claim.Status.IN_PROGRESS) {
                statusProcessingImageButton.visibility = View.INVISIBLE
            }

            titleTextView.text = claim.claim.title
            executorNameTextView.text = if (claim.executor != null) {
                Utils.fullUserNameGenerator(
                    claim.executor.lastName.toString(),
                    claim.executor.firstName.toString(),
                    claim.executor.middleName.toString()
                )
            } else {
                getText(R.string.not_assigned)
            }

            planeDateTextView.text =
                claim.claim.planExecuteDate?.let { Utils.showDateTimeInOne(it) }

            statusLabelTextView.text = displayingStatusOfClaim(claim.claim.status!!)

            descriptionTextView.text = claim.claim.description
            authorNameTextView.text = Utils.fullUserNameGenerator(
                claim.creator.lastName.toString(),
                claim.creator.firstName.toString(),
                claim.creator.middleName.toString()
            )
            createDataTextView.text =
                claim.claim.createDate?.let { Utils.showDateTimeInOne(it) }

            addImageButton.setOnClickListener {
                val action = OpenClaimFragmentDirections
                    .actionOpenClaimFragmentToCreateEditClaimCommentFragment(
                        argComment = null,
                        argClaimId = claim.claim.id!!
                    )
                findNavController().navigate(action)
            }

            closeImageButton.setOnClickListener {
                findNavController().navigateUp()
            }

            statusProcessingImageButton.setOnClickListener {
                statusProcessingMenu.show()
            }

            editProcessingImageButton.setOnClickListener {
                val action = OpenClaimFragmentDirections
                    .actionOpenClaimFragmentToCreateEditClaimFragment(claim)
                findNavController().navigate(action)
            }
        }

        binding.claimCommentsListRecyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            Events.events.collect {
                viewModel.claimCommentUpdatedEvent
                viewModel.commentsData.collect {
                    adapter.submitList(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.commentsData.collect {
                adapter.submitList(it)
            }
        }
    }

    private fun showErrorToast() {
        Toast.makeText(
            requireContext(),
            R.string.error,
            Toast.LENGTH_LONG
        )
    }

    private fun createClaimCommentDialog(
        claim: ClaimWithCreatorAndExecutor,
        user: User
    ) {
        val dialog = CreateCommentDialogFragment.newInstance(
            text = "",
            hint = "Description",
            isMultiline = true
        )
        dialog.onOk = {
            val text = dialog.editText.text
            if (text.isNotBlank()) {
                viewModel.createClaimComment(
                    ClaimComment(
                        claimId = claim.claim.id,
                        description = text.toString(),
                        creatorId = user.id,
                        createDate = LocalDateTime.now().toEpochSecond(
                            ZoneId.of("Europe/Moscow").rules.getOffset(
                                Instant.now()
                            )
                        )
                    )
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.toast_empty_field,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        dialog.show(this.childFragmentManager, "CreateCommentDialog")
    }

    private fun displayingStatusOfClaim(claimStatus: Claim.Status) =
        when (claimStatus) {
            Claim.Status.CANCELLED -> getString(R.string.cancel)
            Claim.Status.EXECUTED -> getString(R.string.executed)
            Claim.Status.IN_PROGRESS -> getString(R.string.in_progress)
            Claim.Status.OPEN -> getString(R.string.status_open)
        }

    private fun statusMenuVisibility(
        claimStatus: Claim.Status,
        statusProcessingMenu: PopupMenu
    ) {
        when (claimStatus) {
            Claim.Status.OPEN -> {
                statusProcessingMenu.menu.setGroupVisible(R.id.open_menu_group, true)
                statusProcessingMenu.menu.setGroupVisible(R.id.in_progress_menu_group, false)
            }
            Claim.Status.IN_PROGRESS -> {
                statusProcessingMenu.menu.setGroupVisible(R.id.open_menu_group, false)
                statusProcessingMenu.menu.setGroupVisible(R.id.in_progress_menu_group, true)
            }
            else -> {
                binding.statusProcessingImageButton.visibility = View.INVISIBLE
                binding.editProcessingImageButton.visibility = View.INVISIBLE
            }
        }
    }
}
