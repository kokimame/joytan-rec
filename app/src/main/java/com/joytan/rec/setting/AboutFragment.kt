package com.joytan.rec.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler
import com.joytan.rec.databinding.FragmentAboutBinding
import kotlinx.android.synthetic.main.activity_about.*

/**
 * About the application, details, goals, version, etc
 */
class AboutFragment : Fragment() {
    private lateinit var viewModel: AboutViewModel
    private val ah = AnalyticsHandler()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        viewModel = AboutViewModel(activity!!)
        viewModel.onCreate()
        val binding = DataBindingUtil.inflate<FragmentAboutBinding>(
                inflater, R.layout.fragment_about, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Analytics
        ah.onCreate(activity!!)
    }
}