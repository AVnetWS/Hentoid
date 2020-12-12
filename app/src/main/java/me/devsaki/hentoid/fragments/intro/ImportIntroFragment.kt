package me.devsaki.hentoid.fragments.intro

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import me.devsaki.hentoid.R
import me.devsaki.hentoid.activities.IntroActivity
import me.devsaki.hentoid.databinding.IncludeImportStepsBinding
import me.devsaki.hentoid.databinding.IntroSlide04Binding
import me.devsaki.hentoid.events.ProcessEvent
import me.devsaki.hentoid.util.FileHelper
import me.devsaki.hentoid.util.ImportHelper
import me.devsaki.hentoid.util.Preferences
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ImportIntroFragment : Fragment(R.layout.intro_slide_04) {

    private var _binding: IntroSlide04Binding? = null
    private var _mergedBinding: IncludeImportStepsBinding? = null
    private val binding get() = _binding!!
    private val mergedBinding get() = _mergedBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = IntroSlide04Binding.inflate(inflater, container, false)
        // We need to manually bind the merged view - it won't work at runtime with the main view alone
        _mergedBinding = IncludeImportStepsBinding.bind(binding.root)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mergedBinding.importStep1Button.setOnClickListener { ImportHelper.openFolderPicker(this, false) }
        mergedBinding.importStep1Button.visibility = View.VISIBLE

        binding.skipBtn.setOnClickListener { askSkip() }
    }

    // Callback from the directory chooser
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (ImportHelper.processPickerResult(activity as Activity, requestCode, resultCode, data)) {
            ImportHelper.Result.OK_EMPTY_FOLDER -> nextStep()
            ImportHelper.Result.OK_LIBRARY_DETECTED -> updateOnSelectFolder() // Import service is already launched by the Helper; nothing else to do
            ImportHelper.Result.OK_LIBRARY_DETECTED_ASK -> {
                updateOnSelectFolder()
                ImportHelper.showExistingLibraryDialog(requireContext()) { onCancelExistingLibraryDialog() }
            }
            ImportHelper.Result.CANCELED -> Snackbar.make(binding.main, R.string.import_canceled, BaseTransientBottomBar.LENGTH_LONG).show()
            ImportHelper.Result.INVALID_FOLDER -> Snackbar.make(binding.main, R.string.import_invalid, BaseTransientBottomBar.LENGTH_LONG).show()
            ImportHelper.Result.APP_FOLDER -> Snackbar.make(binding.main, R.string.import_invalid, BaseTransientBottomBar.LENGTH_LONG).show()
            ImportHelper.Result.DOWNLOAD_FOLDER -> Snackbar.make(binding.main, R.string.import_download_folder, BaseTransientBottomBar.LENGTH_LONG).show()
            ImportHelper.Result.CREATE_FAIL -> Snackbar.make(binding.main, R.string.import_create_fail, BaseTransientBottomBar.LENGTH_LONG).show()
            ImportHelper.Result.OTHER -> Snackbar.make(binding.main, R.string.import_other, BaseTransientBottomBar.LENGTH_LONG).show()
        }
    }

    private fun updateOnSelectFolder() {
        mergedBinding.importStep1Button.visibility = View.INVISIBLE
        mergedBinding.importStep1Folder.text = FileHelper.getFullPathFromTreeUri(requireContext(), Uri.parse(Preferences.getStorageUri()), true)
        mergedBinding.importStep1Check.visibility = View.VISIBLE
        mergedBinding.importStep2.visibility = View.VISIBLE
        mergedBinding.importStep2Bar.isIndeterminate = true
        binding.skipBtn.visibility = View.INVISIBLE
    }

    private fun onCancelExistingLibraryDialog() {
        // Revert back to initial state where only the "Select folder" button is visible
        mergedBinding.importStep1Button.visibility = View.VISIBLE
        mergedBinding.importStep1Folder.text = ""
        mergedBinding.importStep1Check.visibility = View.INVISIBLE
        mergedBinding.importStep2.visibility = View.INVISIBLE
        binding.skipBtn.visibility = View.VISIBLE
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMigrationEvent(event: ProcessEvent) {
        val progressBar: ProgressBar = if (2 == event.step) mergedBinding.importStep2Bar else mergedBinding.importStep3Bar
        if (ProcessEvent.EventType.PROGRESS == event.eventType) {
            if (event.elementsTotal > -1) {
                progressBar.isIndeterminate = false
                progressBar.max = event.elementsTotal
                progressBar.progress = event.elementsOK + event.elementsKO
            } else progressBar.isIndeterminate = true
            if (3 == event.step) {
                mergedBinding.importStep2Check.visibility = View.VISIBLE
                mergedBinding.importStep3.visibility = View.VISIBLE
                mergedBinding.importStep3Text.text = resources.getString(R.string.api29_migration_step3, event.elementsKO + event.elementsOK, event.elementsTotal)
            } else if (4 == event.step) {
                mergedBinding.importStep3Check.visibility = View.VISIBLE
                mergedBinding.importStep4.visibility = View.VISIBLE
            }
        } else if (ProcessEvent.EventType.COMPLETE == event.eventType) {
            if (2 == event.step) {
                mergedBinding.importStep2Check.visibility = View.VISIBLE
                mergedBinding.importStep3.visibility = View.VISIBLE
            } else if (3 == event.step) {
                mergedBinding.importStep3Text.text = resources.getString(R.string.api29_migration_step3, event.elementsTotal, event.elementsTotal)
                mergedBinding.importStep3Check.visibility = View.VISIBLE
                mergedBinding.importStep4.visibility = View.VISIBLE
            } else if (4 == event.step) {
                mergedBinding.importStep4Check.visibility = View.VISIBLE
                nextStep()
            }
        }
    }

    private fun askSkip() {
        val materialDialog: AlertDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.slide_04_skip_title)
                .setMessage(R.string.slide_04_skip_msg)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int -> nextStep() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()

        materialDialog.setIcon(R.drawable.ic_warning)
        materialDialog.show()
    }

    private fun nextStep() {
        val parentActivity = context as IntroActivity
        parentActivity.nextStep()
        binding.skipBtn.visibility = View.VISIBLE
    }
}