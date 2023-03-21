package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleObserver

abstract class BaseFragment<DB : ViewDataBinding, VM : BaseViewModel> :
    DaggerFragment(), LifecycleObserver {

    /** binding onViewCreated() 이후에 호출해서 사용 */
    private var _binding: DB? = null
    val binding: DB
        get() =
            _binding ?: throw RuntimeException("${this::class.java.simpleName} binding is null")

    abstract val viewModel: VM
    abstract val layoutResId: Int

    open val useBackPressedCallback = true


    open fun initArgs(data: Bundle) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { initArgs(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        return _binding?.root
    }

    abstract fun initDataBinding()
    abstract fun initView()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (useBackPressedCallback)
            requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding?.lifecycleOwner = viewLifecycleOwner

        initDataBinding()
        initView()
    }

    open fun onFragmentResume() {}

    override fun onResume() {
        super.onResume()
        onFragmentResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        if (useBackPressedCallback)
            backPressedCallback.remove()

        super.onDestroy()
    }

    /** Permission */
    private var reqPermissionType: PermissionType? = null

    open fun onPermissionGranted(type: PermissionType) {}
    open fun onPermissionDenied(type: PermissionType) {}
    open fun onPermissionRationale(type: PermissionType) {}

    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            Timber.d("${reqPermissionType?.name} ${if (allGranted) "all granted" else "denied"}")

            reqPermissionType?.let { permissionType ->
                if (allGranted) onPermissionGranted(permissionType)
                else onPermissionDenied(permissionType)
            }

            reqPermissionType = null
        }

    fun requestPermission(type: PermissionType) {
        if (reqPermissionType != null)
            throw RuntimeException("There is a Request Permission in progress")

        activity?.let {
            when (PermissionUtil.getPermissionStatus(it, type)) {
                PermissionStatus.GRANTED -> onPermissionGranted(type)
                PermissionStatus.RATIONALE -> onPermissionRationale(type)
                PermissionStatus.NO_REQUEST -> {
                    reqPermissionType = type
                    requestPermissionLauncher.launch(type.permissions)
                }
            }
        }
    }


    /** BackPressed Event */
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (onBackPressed()) return

            activity?.onBackPressed()
        }
    }

    /** onBackPressed override
     * @return Boolean false인 경우, activity.onBackPressed() 호출 */
    open fun onBackPressed() = false

}