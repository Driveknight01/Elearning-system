package com.elearning.smartflash.ui.set;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.elearning.smartflash.R;

import java.util.List;

/**
 * RecyclerView Adapter for {@link CreateEditSetActivity} containing a single Header item
 * (Title, Scan document, + Description) followed by editable flashcard items.
 */
public class FlashcardEditAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_CARD = 1;

    public interface Callbacks {
        void onTitleChanged(String title);
        void onDescriptionChanged(String description);
        void onDescriptionShown();
        void onScanDocumentClicked();
        void onHeaderFocused();
        void onCardFocused(int cardNumberOneBased, int totalCards);
        void onSelectTermLanguageClicked(int cardIndex);
        void onSelectDefLanguageClicked(int cardIndex);
        void onCardCameraClicked(int cardIndex);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    private final CreateEditSetViewModel viewModel;
    private final Callbacks callbacks;
    private int pendingFocusCardIndex = -1;

    public FlashcardEditAdapter(CreateEditSetViewModel viewModel, Callbacks callbacks) {
        this.viewModel = viewModel;
        this.callbacks = callbacks;
        setHasStableIds(true);
    }

    public void requestFocusForCard(int cardIndex) {
        this.pendingFocusCardIndex = cardIndex;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_CARD;
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) {
            return 0L;
        }
        return viewModel.getCards().get(position - 1).getLocalId();
    }

    @Override
    public int getItemCount() {
        return 1 + viewModel.getCards().size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_create_set_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_flashcard_edit, parent, false);
            return new CardViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder headerHolder) {
            headerHolder.bind();
        } else if (holder instanceof CardViewHolder cardHolder) {
            int cardIndex = position - 1;
            cardHolder.bind(cardIndex);
            if (pendingFocusCardIndex == cardIndex) {
                pendingFocusCardIndex = -1;
                cardHolder.etTerm.post(() -> {
                    cardHolder.etTerm.requestFocus();
                    cardHolder.etTerm.setSelection(cardHolder.etTerm.getText().length());
                });
            }
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {

        final EditText etTitle;
        final View viewTitleUnderline;
        final LinearLayout layoutDescriptionContainer;
        final EditText etDescription;
        final View viewDescUnderline;
        final LinearLayout btnScanDocument;
        final TextView btnAddDescription;

        private TextWatcher titleWatcher;
        private TextWatcher descWatcher;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            etTitle = itemView.findViewById(R.id.et_title);
            viewTitleUnderline = itemView.findViewById(R.id.view_title_underline);
            layoutDescriptionContainer = itemView.findViewById(R.id.layout_description_container);
            etDescription = itemView.findViewById(R.id.et_description);
            viewDescUnderline = itemView.findViewById(R.id.view_desc_underline);
            btnScanDocument = itemView.findViewById(R.id.btn_scan_document);
            btnAddDescription = itemView.findViewById(R.id.btn_add_description);
        }

        void bind() {
            if (titleWatcher != null) {
                etTitle.removeTextChangedListener(titleWatcher);
            }
            if (descWatcher != null) {
                etDescription.removeTextChangedListener(descWatcher);
            }

            if (!etTitle.getText().toString().equals(viewModel.getTitle())) {
                etTitle.setText(viewModel.getTitle());
            }
            if (!etDescription.getText().toString().equals(viewModel.getDescription())) {
                etDescription.setText(viewModel.getDescription());
            }

            boolean descVisible = viewModel.isDescriptionVisible();
            layoutDescriptionContainer.setVisibility(descVisible ? View.VISIBLE : View.GONE);
            btnAddDescription.setVisibility(descVisible ? View.GONE : View.VISIBLE);

            titleWatcher = new SimpleTextWatcher(text -> callbacks.onTitleChanged(text));
            descWatcher = new SimpleTextWatcher(text -> callbacks.onDescriptionChanged(text));
            etTitle.addTextChangedListener(titleWatcher);
            etDescription.addTextChangedListener(descWatcher);

            etTitle.setOnFocusChangeListener((v, hasFocus) -> {
                updateUnderline(viewTitleUnderline, hasFocus);
                if (hasFocus) {
                    callbacks.onHeaderFocused();
                }
            });
            updateUnderline(viewTitleUnderline, etTitle.hasFocus());
            etDescription.setOnFocusChangeListener((v, hasFocus) -> {
                updateUnderline(viewDescUnderline, hasFocus);
                if (hasFocus) {
                    callbacks.onHeaderFocused();
                }
            });
            updateUnderline(viewDescUnderline, etDescription.hasFocus());

            btnScanDocument.setOnClickListener(v -> callbacks.onScanDocumentClicked());

            btnAddDescription.setOnClickListener(v -> {
                viewModel.setDescriptionVisible(true);
                layoutDescriptionContainer.setVisibility(View.VISIBLE);
                btnAddDescription.setVisibility(View.GONE);
                callbacks.onDescriptionShown();
                etDescription.requestFocus();
            });
        }
    }

    class CardViewHolder extends RecyclerView.ViewHolder {

        final EditText etTerm;
        final ImageView ivDragHandle;
        final View viewTermUnderline;
        final TextView tvTermSelectLanguage;
        final EditText etDefinition;
        final ImageButton btnDefCamera;
        final View viewDefUnderline;
        final TextView tvDefSelectLanguage;
        final ImageView ivCardImage;

        private TextWatcher termWatcher;
        private TextWatcher defWatcher;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            etTerm = itemView.findViewById(R.id.et_term);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            viewTermUnderline = itemView.findViewById(R.id.view_term_underline);
            tvTermSelectLanguage = itemView.findViewById(R.id.tv_term_select_language);
            etDefinition = itemView.findViewById(R.id.et_definition);
            btnDefCamera = itemView.findViewById(R.id.btn_def_camera);
            viewDefUnderline = itemView.findViewById(R.id.view_def_underline);
            tvDefSelectLanguage = itemView.findViewById(R.id.tv_def_select_language);
            ivCardImage = itemView.findViewById(R.id.iv_card_image);
        }

        void bind(int cardIndex) {
            List<EditableCardItem> cards = viewModel.getCards();
            if (cardIndex < 0 || cardIndex >= cards.size()) {
                return;
            }
            EditableCardItem card = cards.get(cardIndex);

            if (termWatcher != null) {
                etTerm.removeTextChangedListener(termWatcher);
            }
            if (defWatcher != null) {
                etDefinition.removeTextChangedListener(defWatcher);
            }

            if (!etTerm.getText().toString().equals(card.getTerm())) {
                etTerm.setText(card.getTerm());
            }
            if (!etDefinition.getText().toString().equals(card.getDefinition())) {
                etDefinition.setText(card.getDefinition());
            }

            tvTermSelectLanguage.setText(SetLanguageOptions.getInlineButtonLabel(viewModel.getTermLang()));
            tvDefSelectLanguage.setText(SetLanguageOptions.getInlineButtonLabel(viewModel.getDefLang()));

            if (card.getImageUrl() != null && !card.getImageUrl().trim().isEmpty()) {
                ivCardImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(card.getImageUrl())
                        .centerCrop()
                        .into(ivCardImage);
            } else {
                ivCardImage.setVisibility(View.GONE);
            }

            // Apply initial focus visual state
            applyTermFocusState(etTerm.hasFocus());
            applyDefinitionFocusState(etDefinition.hasFocus());

            termWatcher = new SimpleTextWatcher(text -> {
                int pos = getBindingAdapterPosition();
                if (pos > 0 && pos - 1 < viewModel.getCards().size()) {
                    viewModel.getCards().get(pos - 1).setTerm(text);
                }
            });
            defWatcher = new SimpleTextWatcher(text -> {
                int pos = getBindingAdapterPosition();
                if (pos > 0 && pos - 1 < viewModel.getCards().size()) {
                    viewModel.getCards().get(pos - 1).setDefinition(text);
                }
            });
            etTerm.addTextChangedListener(termWatcher);
            etDefinition.addTextChangedListener(defWatcher);

            etTerm.setOnFocusChangeListener((v, hasFocus) -> {
                applyTermFocusState(hasFocus);
                if (hasFocus) {
                    int pos = getBindingAdapterPosition();
                    if (pos > 0) {
                        callbacks.onCardFocused(pos, viewModel.getCards().size());
                    }
                }
            });

            etDefinition.setOnFocusChangeListener((v, hasFocus) -> {
                applyDefinitionFocusState(hasFocus);
                if (hasFocus) {
                    int pos = getBindingAdapterPosition();
                    if (pos > 0) {
                        callbacks.onCardFocused(pos, viewModel.getCards().size());
                    }
                }
            });

            tvTermSelectLanguage.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos > 0) {
                    callbacks.onSelectTermLanguageClicked(pos - 1);
                }
            });

            tvDefSelectLanguage.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos > 0) {
                    callbacks.onSelectDefLanguageClicked(pos - 1);
                }
            });

            btnDefCamera.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos > 0) {
                    callbacks.onCardCameraClicked(pos - 1);
                }
            });

            ivDragHandle.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                callbacks.onStartDrag(this);
                return true;
            });
        }

        private void applyTermFocusState(boolean focused) {
            updateUnderline(viewTermUnderline, focused);
            tvTermSelectLanguage.setVisibility(focused ? View.VISIBLE : View.GONE);
        }

        private void applyDefinitionFocusState(boolean focused) {
            updateUnderline(viewDefUnderline, focused);
            btnDefCamera.setVisibility(focused ? View.VISIBLE : View.GONE);
            tvDefSelectLanguage.setVisibility(focused ? View.VISIBLE : View.GONE);
        }
    }

    private static void updateUnderline(View underline, boolean focused) {
        Context context = underline.getContext();
        ViewGroup.LayoutParams lp = underline.getLayoutParams();
        float dp = focused ? 3.5f : 1.5f;
        lp.height = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
        underline.setLayoutParams(lp);
        underline.setBackgroundColor(ContextCompat.getColor(
                context, focused ? R.color.sf_underline_focus : R.color.sf_underline_idle));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        interface OnChanged {
            void onChanged(String text);
        }

        private final OnChanged listener;

        SimpleTextWatcher(OnChanged listener) {
            this.listener = listener;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            listener.onChanged(s != null ? s.toString() : "");
        }
    }
}
