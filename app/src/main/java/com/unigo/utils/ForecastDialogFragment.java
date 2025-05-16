package com.unigo.utils;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unigo.R;
import com.unigo.adapters.ForecastAdapter;

import java.util.ArrayList;
import java.util.List;

public class ForecastDialogFragment extends DialogFragment {
    private static final String ARG_LIST = "arg_list";
    private List<ForecastDay> list;

    public static ForecastDialogFragment newInstance(ArrayList<ForecastDay> list) {
        ForecastDialogFragment f = new ForecastDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_LIST, list);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        list = getArguments().getParcelableArrayList(ARG_LIST);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_Unigo_MaterialAlertDialog);
        View view = requireActivity().getLayoutInflater()
                .inflate(R.layout.fragment_forecast_dialog, null);
        RecyclerView rv = view.findViewById(R.id.rvForecast);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new ForecastAdapter(list, getContext()));
        builder.setView(view)
                .setTitle(R.string.forecast_title)
                .setPositiveButton(android.R.string.ok, null);
        return builder.create();
    }
}

