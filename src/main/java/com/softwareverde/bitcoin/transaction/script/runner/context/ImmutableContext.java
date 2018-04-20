package com.softwareverde.bitcoin.transaction.script.runner.context;

import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.input.TransactionInput;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.constable.Const;

public class ImmutableContext implements Context, Const {
    protected Long _blockHeight;
    protected Transaction _transaction;

    protected Integer _transactionInputIndex;
    protected TransactionInput _transactionInput;
    protected TransactionOutput _transactionOutput;

    protected Integer _currentScriptIndex;
    protected Integer _scriptLastCodeSeparatorIndex;

    public ImmutableContext(final Context context) {
        _blockHeight = context.getBlockHeight();
        _transaction = context.getTransaction();
        _transactionInputIndex = context.getTransactionInputIndex();
        _transactionInput = context.getTransactionInput();
        _transactionOutput = context.getTransactionOutput();

        _currentScriptIndex = context.getCurrentScriptIndex();
        _scriptLastCodeSeparatorIndex = context.getScriptLastCodeSeparatorIndex();
    }

    @Override
    public Long getBlockHeight() {
        return _blockHeight;
    }

    @Override
    public TransactionInput getTransactionInput() {
        return _transactionInput;
    }

    @Override
    public TransactionOutput getTransactionOutput() {
        return _transactionOutput;
    }

    @Override
    public Transaction getTransaction() {
        return _transaction;
    }

    @Override
    public Integer getTransactionInputIndex() {
        return _transactionInputIndex;
    }

    @Override
    public Integer getCurrentScriptIndex() {
        return _currentScriptIndex;
    }

    @Override
    public Integer getScriptLastCodeSeparatorIndex() {
        return _scriptLastCodeSeparatorIndex;
    }

    @Override
    public ImmutableContext asConst() {
        return this;
    }
}