package com.teamnexters.lastwednesday.fragment.adapter;

import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ramotion.foldingcell.FoldingCell;
import com.teamnexters.lastwednesday.MainActivity;
import com.teamnexters.lastwednesday.R;
import com.teamnexters.lastwednesday.databinding.ItemTicketBinding;
import com.teamnexters.lastwednesday.model.Ticket;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by JY on 2018-01-12.
 */

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> implements View.OnClickListener {

    private Context context;

    private List<Ticket> dataSet;
    private List<Ticket> selectedList;

    private PublishSubject<View> longClickSubject;
    private PublishSubject<Ticket> checkedSubject;

    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            ((MainActivity) context).actionModeState(true);

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.delete_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    deleteSelectedItems();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectedList.clear();
            ((MainActivity) context).actionModeState(false);
            mActionMode = null;
            changeSelectState(false);
            changeCheckBoxVisibility(false);
        }
    };

    public TicketAdapter(Context context) {
        this.context = context;
        this.dataSet = new ArrayList<>();
        this.selectedList = new ArrayList<>();
        this.checkedSubject = PublishSubject.create();
        this.longClickSubject = PublishSubject.create();
    }

    @Override
    public TicketViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        itemView.setOnClickListener(this);

        return new TicketViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TicketViewHolder holder, int position) {
        Ticket ticket = dataSet.get(position);
        holder.binding.setObj(ticket);
        holder.binding.cellTitleTicket.imageTitleTicket.setImageDrawable(context.getResources().getDrawable(ticket.getPoster()));
        holder.getLongClickObservable().subscribe(longClickSubject);
        holder.getCheckedObservable(ticket).subscribe(checkedSubject);
    }


    @Override
    public int getItemCount() {
        return (dataSet != null ? dataSet.size() : 0);
    }

    public void onCheckedChangeEventPublish() { //체크박스의 체크상태가 변할때 실행
        checkedSubject
                .subscribe(data -> {
                    addSelectedItem(data);
                    if (mActionMode != null) {
                        mActionMode.setTitle(Html.fromHtml("<font color = '#5fc8e4' >" + selectedList.size() + "</font>" + context.getString(R.string.select)));
                        //선택된 항목의 개수를 액션바에 보여줌
                    }
                });
    }

    private void changeSelectState(boolean state) { //체크박스 선택상태 변경.
        Observable.fromIterable(dataSet)
                .filter(val -> val.isSelected() != state)
                .subscribe(data -> {
                    data.setSelected(state);
                    notifyDataSetChanged();
                });
    }

    private void addSelectedItem(Ticket item) {
        if (!selectedList.contains(item) && item.isSelected()) {
            selectedList.add(item);
        } else if (!item.isSelected() && selectedList.contains(item)) {
            selectedList.remove(item);
        }
    }

    public void onLongClickEventPublish() {
        longClickSubject
                .subscribe(v -> {
                    if (mActionMode == null) {
                        changeCheckBoxVisibility(true);
                        mActionMode = ((MainActivity) context).startActionMode(mActionModeCallback);
                    }
                });
    }

    private void changeCheckBoxVisibility(boolean state) {
        Observable.fromIterable(dataSet)
                .filter(data -> data.isLongClicked() != state)
                .subscribe(data -> {
                    data.setLongClicked(state);
                    notifyDataSetChanged();
                });
    }

    public void updateDataSet(List<Ticket> items) {
        this.dataSet.addAll(items);
        notifyDataSetChanged();
    }

    private void deleteSelectedItems() {
        this.dataSet.removeAll(selectedList);
        notifyDataSetChanged();
    }

    public void clearDataSet() {
        this.dataSet.clear();
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if (mActionMode == null) {
            ((FoldingCell) v).toggle(false);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        longClickSubject.onComplete();
        checkedSubject.onComplete();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ItemTicketBinding binding;
        TextView btn_rate_1;
        TextView btn_comment_1;
        TextView btn_rate_2;
        TextView btn_comment_2;

        private TicketViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);

            btn_rate_1 = (TextView) itemView.findViewById(R.id.text_title_star);
            btn_comment_1 = (TextView) itemView.findViewById(R.id.text_title_comment);

            btn_rate_2 = (TextView) itemView.findViewById(R.id.text_content_star);
            btn_comment_2 = (TextView) itemView.findViewById(R.id.text_content_comment);

            btn_rate_1.setOnClickListener(this);
            btn_comment_1.setOnClickListener(this);

            btn_rate_2.setOnClickListener(this);
            btn_comment_2.setOnClickListener(this);
        }

        private Observable<Ticket> getCheckedObservable(Ticket item) { //checkbox observable
            return Observable.create(e ->
                    binding.cellTitleTicket.checkTicket.setOnCheckedChangeListener(
                            (compoundBtn, b) -> {
                                item.setSelected(b);
                                e.onNext(item);
                            }
                    ));
        }

        private Observable<View> getLongClickObservable() {
            return Observable.create(e -> {
                itemView.setOnLongClickListener(view -> {
                    e.onNext(itemView);
                    return true;
                });
            });
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.text_title_star:
                    Dialog dialog_star_1 = new Dialog(itemView.getContext(), R.style.custom_dialog);
                    dialog_star_1.setContentView(R.layout.dialog_stared);
                    RatingBar ratingBar_1 = (RatingBar) dialog_star_1.findViewById(R.id.rating_star);
                    TextView btn_check_1 = (TextView) dialog_star_1.findViewById(R.id.btn_check);
                    TextView btn_cancle_1 = (TextView) dialog_star_1.findViewById(R.id.btn_cancle);

                    btn_check_1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(itemView.getContext(), String.valueOf(ratingBar_1.getRating()), Toast.LENGTH_LONG).show();
                            dialog_star_1.dismiss();
                        }
                    });

                    btn_cancle_1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog_star_1.dismiss();
                        }
                    });

                    dialog_star_1.show();
                    break;
                case R.id.text_title_comment:
                    Dialog dialog_comment_1 = new Dialog(itemView.getContext(), R.style.custom_dialog);
                    dialog_comment_1.setContentView(R.layout.dialog_comment);
                    ImageView btn_close_1 = (ImageView) dialog_comment_1.findViewById(R.id.btn_close);
                    TextView btn_complete_1 = (TextView) dialog_comment_1.findViewById(R.id.btn_complete);
                    EditText edit_comment_1 = (EditText) dialog_comment_1.findViewById(R.id.edit_comment);

                    btn_close_1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog_comment_1.dismiss();
                        }
                    });

                    btn_complete_1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(itemView.getContext(), edit_comment_1.getText(), Toast.LENGTH_LONG).show();
                            dialog_comment_1.dismiss();
                        }
                    });

                    edit_comment_1.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (count == 0) {
                                btn_complete_1.setTextColor(itemView.getResources().getColor(R.color.cGray2));
                            } else {
                                btn_complete_1.setTextColor(itemView.getResources().getColor(R.color.cSkyblue));
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });

                    dialog_comment_1.show();
                    break;
                case R.id.text_content_star:
                    Dialog dialog_star_2 = new Dialog(itemView.getContext(), R.style.custom_dialog);
                    dialog_star_2.setContentView(R.layout.dialog_stared);
                    RatingBar ratingBar_2 = (RatingBar) dialog_star_2.findViewById(R.id.rating_star);
                    TextView btn_check_2 = (TextView) dialog_star_2.findViewById(R.id.btn_check);
                    TextView btn_cancle_2 = (TextView) dialog_star_2.findViewById(R.id.btn_cancle);

                    btn_check_2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(itemView.getContext(), String.valueOf(ratingBar_2.getRating()), Toast.LENGTH_LONG).show();
                            dialog_star_2.dismiss();
                        }
                    });

                    btn_cancle_2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog_star_2.dismiss();
                        }
                    });
                    dialog_star_2.show();
                    break;
                case R.id.text_content_comment:
                    Dialog dialog_comment_2 = new Dialog(itemView.getContext(), R.style.custom_dialog);
                    dialog_comment_2.setContentView(R.layout.dialog_comment);
                    ImageView btn_close_2 = (ImageView) dialog_comment_2.findViewById(R.id.btn_close);
                    TextView btn_complete_2 = (TextView) dialog_comment_2.findViewById(R.id.btn_complete);
                    EditText edit_comment_2 = (EditText) dialog_comment_2.findViewById(R.id.edit_comment);

                    btn_close_2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog_comment_2.dismiss();
                        }
                    });

                    btn_complete_2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(itemView.getContext(), edit_comment_2.getText(), Toast.LENGTH_LONG).show();
                            dialog_comment_2.dismiss();
                        }
                    });

                    edit_comment_2.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (count == 0) {
                                btn_complete_2.setTextColor(itemView.getResources().getColor(R.color.cGray2));
                            } else {
                                btn_complete_2.setTextColor(itemView.getResources().getColor(R.color.cSkyblue));
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });

                    dialog_comment_2.show();
                    break;
            }
        }
    }

}
