/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package im.vector.riotx.features.roommemberprofile.devices

import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.commitTransaction
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet
import javax.inject.Inject
import kotlin.reflect.KClass

class DeviceListBottomSheet : VectorBaseBottomSheetDialogFragment() {

    override fun getLayoutResId() = R.layout.bottom_sheet_with_fragments

    private val viewModel: DeviceListBottomSheetViewModel by fragmentViewModel(DeviceListBottomSheetViewModel::class)

    @Inject lateinit var viewModelFactory: DeviceListBottomSheetViewModel.Factory

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.observeViewEvents {
            when (it) {
                is DeviceListBottomSheetViewEvents.Verify -> {
                    VerificationBottomSheet.withArgs(
                            roomId = null,
                            otherUserId = it.userId,
                            transactionId = it.txID
                    ).show(requireActivity().supportFragmentManager, "REQPOP")
                }
            }.exhaustive
        }
    }

    private val onKeyListener = DialogInterface.OnKeyListener { _, keyCode, _ ->
        withState(viewModel) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (it.selectedDevice != null) {
                    viewModel.selectDevice(null)
                    return@withState true
                } else {
                    return@withState false
                }
            }
            return@withState false
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener(onKeyListener)
    }

    override fun onPause() {
        super.onPause()
        dialog?.setOnKeyListener(null)
    }

    override fun invalidate() = withState(viewModel) {
        super.invalidate()
        if (it.selectedDevice == null) {
            showFragment(DeviceListFragment::class, arguments ?: Bundle())
        } else {
            showFragment(DeviceTrustInfoActionFragment::class, arguments ?: Bundle())
        }
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, bundle: Bundle) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            childFragmentManager.commitTransaction {
                replace(R.id.bottomSheetFragmentContainer,
                        fragmentClass.java,
                        bundle,
                        fragmentClass.simpleName
                )
            }
        }
    }

    companion object {
        fun newInstance(userId: String): DeviceListBottomSheet {
            val args = Bundle()
            args.putString(MvRx.KEY_ARG, userId)
            return DeviceListBottomSheet().apply { arguments = args }
        }
    }
}
