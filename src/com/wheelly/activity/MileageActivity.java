/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - adding bill filtering parameters
 ******************************************************************************/
package com.wheelly.activity;

import static ru.orangesoftware.financisto.utils.Utils.text;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.*;

import com.wheelly.R;
import ru.orangesoftware.financisto.activity.AbstractActivity;
import ru.orangesoftware.financisto.adapter.EntityEnumAdapter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.AmountInput.OnAmountChangedListener;

public class MileageActivity extends AbstractActivity {
	
	public static final String MILEAGE_ID_EXTRA = "id";
	
	private static final int MENU_TURN_GPS_ON = Menu.FIRST;

    private AutoCompleteTextView payeeText;
    private SimpleCursorAdapter payeeAdapter;
	private TextView differenceText;
	private boolean isUpdateBalanceMode = false;
    private boolean isShowPayee = true;
	private long currentBalance;
	private Utils u;

	public MileageActivity() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.setContentView(R.layout.mileage);
		
		LinearLayout layout = (LinearLayout)findViewById(R.id.list);
		////
		
		Button bSave = (Button) findViewById(R.id.bSave);
		bSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                saveAndFinish();
            }

        });

        final boolean isEdit = transaction.id > 0;
		Button bSaveAndNew = (Button)findViewById(R.id.bSaveAndNew);
        if (isEdit) {
            bSaveAndNew.setText(R.string.cancel);
        }
		bSaveAndNew.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (isEdit) {
                    setResult(RESULT_CANCELED);
                    finish();
                } else {
                    if (saveAndFinish()) {
                        startActivityForResult(intent, -1);
                    }
                }
            }
        });
	}
	
	@Override
	protected void internalOnCreate() {
		u = new Utils(this);
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(CURRENT_BALANCE_EXTRA)) {
				currentBalance = intent.getLongExtra(CURRENT_BALANCE_EXTRA, 0);
				isUpdateBalanceMode = true;
			}
		}
	}

	@Override
	protected void createListNodes(LinearLayout layout) {
		//account
		accountText = x.addListNode(layout, R.id.account, R.string.account, R.string.select_account);
        //payee
        isShowPayee = MyPreferences.isShowPayee(this);
        if (isShowPayee) {
            payeeAdapter = TransactionUtils.createPayeeAdapter(this, db);
            payeeText = new AutoCompleteTextView(this);
            payeeText.setThreshold(1);
            payeeText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        payeeText.setAdapter(payeeAdapter);
                        payeeText.selectAll();
                    }
                }
            });
            x.addEditNode(layout, R.string.payee, payeeText);
        }
		//category
		categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
		//amount
		amountInput = new AmountInput(this);
		amountInput.setOwner(this);
		x.addEditNode(layout, isUpdateBalanceMode ? R.string.new_balance : R.string.amount, amountInput);
		// difference
		if (isUpdateBalanceMode) {
			differenceText = x.addInfoNode(layout, -1, R.string.difference, "0");
			amountInput.setAmount(currentBalance);
			amountInput.setOnAmountChangedListener(new OnAmountChangedListener(){
				@Override
				public void onAmountChanged(long oldAmount, long newAmount) {
					long balanceDifference = newAmount-currentBalance;
					u.setAmountText(differenceText, amountInput.getCurrency(), balanceDifference, true);
				}
			});
            if (currentBalance >= 0) {
                amountInput.setIncome();
            } else {
                amountInput.setExpense();
            }
		}
	}

    protected void switchIncomeExpenseButton(Category category) {
        if (!isUpdateBalanceMode) {
            if (category.isIncome()) {
                amountInput.setIncome();
            } else {
                amountInput.setExpense();
            }
        }
    }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
			accountText.requestFocusFromTouch();
		}
	}

	@Override
	protected boolean onOKClicked() {
		if (checkSelectedId(selectedAccountId, R.string.select_account)) {
			updateTransactionFromUI();
			return true;
		}
		return false;
	}

	private void updateTransactionFromUI() {
		updateTransactionFromUI(transaction);
        if (isShowPayee) {
            transaction.payeeId = db.insertPayee(text(payeeText));
        }
		transaction.fromAccountId = selectedAccountId;
		long amount = amountInput.getAmount();
		if (isUpdateBalanceMode) {
			amount -= currentBalance;
		}
		transaction.fromAmount = amount;
	}
}