package com.softwareverde.bitcoin.transaction.validator;

import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionInflater;
import com.softwareverde.bitcoin.transaction.input.TransactionInput;
import com.softwareverde.bitcoin.transaction.input.TransactionInputInflater;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.bitcoin.transaction.output.TransactionOutputInflater;
import com.softwareverde.bitcoin.transaction.script.Script;
import com.softwareverde.bitcoin.transaction.script.locking.ImmutableLockingScript;
import com.softwareverde.bitcoin.transaction.script.locking.LockingScript;
import com.softwareverde.bitcoin.transaction.script.opcode.CryptographicOperation;
import com.softwareverde.bitcoin.transaction.script.opcode.Operation;
import com.softwareverde.bitcoin.transaction.script.opcode.OperationInflater;
import com.softwareverde.bitcoin.transaction.script.runner.ScriptRunner;
import com.softwareverde.bitcoin.transaction.script.runner.context.MutableContext;
import com.softwareverde.bitcoin.transaction.script.stack.Stack;
import com.softwareverde.bitcoin.transaction.script.stack.Value;
import com.softwareverde.bitcoin.transaction.script.unlocking.ImmutableUnlockingScript;
import com.softwareverde.bitcoin.transaction.script.unlocking.UnlockingScript;
import com.softwareverde.bitcoin.util.bytearray.ByteArrayReader;
import com.softwareverde.constable.list.List;
import com.softwareverde.util.HexUtil;
import org.junit.Assert;
import org.junit.Test;

public class HistoricTransactionsTests {
    @Test
    public void should_verify_multisig_transaction_EB3B82C0884E3EFA6D8B0BE55B4915EB20BE124C9766245BCC7F34FDAC32BCCB_1() throws Exception {
        // Setup
        final Stack stack = new Stack();
        {
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("00000000")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("30440220276D6DAD3DEFA37B5F81ADD3992D510D2F44A317FD85E04F93A1E2DAEA64660202200F862A0DA684249322CEB8ED842FB8C859C0CB94C81E1C5308B4868157A428EE01")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("01000000")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("0232ABDC893E7F0631364D7FD01CB33D24DA45329A00357B3A7886211AB414D55A")));
            stack.push(Value.fromBytes(HexUtil.hexStringToByteArray("01000000")));
        }

        final MutableContext mutableContext = new MutableContext();
        {
            final TransactionInflater transactionInflater = new TransactionInflater();
            final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("01000000024DE8B0C4C2582DB95FA6B3567A989B664484C7AD6672C85A3DA413773E63FDB8000000006B48304502205B282FBC9B064F3BC823A23EDCC0048CBB174754E7AA742E3C9F483EBE02911C022100E4B0B3A117D36CAB5A67404DDDBF43DB7BEA3C1530E0FE128EBC15621BD69A3B0121035AA98D5F77CD9A2D88710E6FC66212AFF820026F0DAD8F32D1F7CE87457DDE50FFFFFFFF4DE8B0C4C2582DB95FA6B3567A989B664484C7AD6672C85A3DA413773E63FDB8010000006F004730440220276D6DAD3DEFA37B5F81ADD3992D510D2F44A317FD85E04F93A1E2DAEA64660202200F862A0DA684249322CEB8ED842FB8C859C0CB94C81E1C5308B4868157A428EE01AB51210232ABDC893E7F0631364D7FD01CB33D24DA45329A00357B3A7886211AB414D55A51AEFFFFFFFF02E0FD1C00000000001976A914380CB3C594DE4E7E9B8E18DB182987BEBB5A4F7088ACC0C62D000000000017142A9BC5447D664C1D0141392A842D23DBA45C4F13B17500000000"));
            final Integer transactionInputIndex = 1;

            final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
            final Integer txOutIndex = 1;
            final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(txOutIndex, HexUtil.hexStringToByteArray("C0C62D000000000017142A9BC5447D664C1D0141392A842D23DBA45C4F13B175"));
            final Integer codeSeparatorIndex = 3;

            final Script unlockingScript;
            {
                final List<TransactionInput> transactionInputs = transaction.getTransactionInputs();
                final TransactionInput transactionInput = transactionInputs.get(transactionInputIndex);
                unlockingScript = transactionInput.getUnlockingScript();
            }

            mutableContext.setCurrentScript(unlockingScript); // Set the current script, since ScriptRunner isn't being used here...
            mutableContext.setTransaction(transaction);
            mutableContext.setTransactionInputIndex(transactionInputIndex);
            mutableContext.setTransactionOutput(transactionOutput);
            mutableContext.setLockingScriptLastCodeSeparatorIndex(codeSeparatorIndex);
        }

        // final CryptographicOperation checkMultisigOperation = new CryptographicOperation((byte) 0xAE, Operation.Opcode.CHECK_MULTISIGNATURE);
        final OperationInflater operationInflater = new OperationInflater();
        final Operation checkMultisigOperation = operationInflater.fromBytes(new ByteArrayReader(new byte[] { (byte) 0xAE }));
        Assert.assertTrue(checkMultisigOperation instanceof CryptographicOperation);

        // Action
        final Boolean shouldContinue = checkMultisigOperation.applyTo(stack, mutableContext);
        final Value lastValue = stack.pop();

        // Assert
        Assert.assertTrue(shouldContinue);
        Assert.assertFalse(stack.didOverflow());
        Assert.assertTrue(lastValue.asBoolean());
    }

    @Test
    public void should_verify_sighashnone_transaction_599E47A8114FE098103663029548811D2651991B62397E057F0C863C2BC9F9EA_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("01000000015F386C8A3842C9A9DCFA9B78BE785A40A7BDA08B64646BE3654301EACCFC8D5E010000008A4730440220BB4FBC495AA23BABB2C2BE4E3FB4A5DFFEFE20C8EFF5940F135649C3EA96444A022004AFCDA966C807BB97622D3EEFEA828F623AF306EF2B756782EE6F8A22A959A2024104F1939AE6B01E849BF05D0ED51FD5B92B79A0E313E3F389C726F11FA3E144D9227B07E8A87C0EE36372E967E090D11B777707AA73EFACABFFFFA285C00B3622D6FFFFFFFF0240420F00000000001976A914660D4EF3A743E3E696AD990364E555C271AD504B88AC2072C801000000001976A91421C43CE400901312A603E4207AADFD742BE8E7DA88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("5F386C8A3842C9A9DCFA9B78BE785A40A7BDA08B64646BE3654301EACCFC8D5E010000008A4730440220BB4FBC495AA23BABB2C2BE4E3FB4A5DFFEFE20C8EFF5940F135649C3EA96444A022004AFCDA966C807BB97622D3EEFEA828F623AF306EF2B756782EE6F8A22A959A2024104F1939AE6B01E849BF05D0ED51FD5B92B79A0E313E3F389C726F11FA3E144D9227B07E8A87C0EE36372E967E090D11B777707AA73EFACABFFFFA285C00B3622D6FFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("60B4D701000000001976A91421C43CE400901312A603E4207AADFD742BE8E7DA88AC"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(178627L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("76A91421C43CE400901312A603E4207AADFD742BE8E7DA88AC"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("4730440220BB4FBC495AA23BABB2C2BE4E3FB4A5DFFEFE20C8EFF5940F135649C3EA96444A022004AFCDA966C807BB97622D3EEFEA828F623AF306EF2B756782EE6F8A22A959A2024104F1939AE6B01E849BF05D0ED51FD5B92B79A0E313E3F389C726F11FA3E144D9227B07E8A87C0EE36372E967E090D11B777707AA73EFACABFFFFA285C00B3622D6"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_pay_to_script_hash_transaction_6A26D2ECB67F27D1FA5524763B49029D7106E91E3CC05743073461A719776192_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("0100000001F6EA284EC7521F8A7D094A6CF4E6873098B90F90725FFD372B343189D7A4089C0100000026255121029B6D2C97B8B7C718C325D7BE3AC30F7C9D67651BCE0C929F55EE77CE58EFCF8451AEFFFFFFFF0130570500000000001976A9145A3ACBC7BBCC97C5FF16F5909C9D7D3FADB293A888AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("F6EA284EC7521F8A7D094A6CF4E6873098B90F90725FFD372B343189D7A4089C0100000026255121029B6D2C97B8B7C718C325D7BE3AC30F7C9D67651BCE0C929F55EE77CE58EFCF8451AEFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("801A06000000000017A91419A7D869032368FD1F1E26E5E73A4AD0E474960E87"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(170095L); // NOTE: P2SH was not activated until Block 173805...
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("A91419A7D869032368FD1F1E26E5E73A4AD0E474960E87"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("255121029B6D2C97B8B7C718C325D7BE3AC30F7C9D67651BCE0C929F55EE77CE58EFCF8451AE"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_pay_to_script_hash_transaction_1CC1ECDF5C05765DF3D1F59FBA24CD01C45464C329B0F0A25AA9883ADFCF7F29_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("01000000014ACC6214C74AF23AE430AFF165004D8E10DB9860B87FFB7E7E703AF97B872203010000009200483045022100BEB926DA7428FA009AC770576342EBD1960939E73584A5D0F3229B58C41E906F022017C0D143077906AFCCF30CAF21F5ECE0BB30E3F708FD4A17F9D9EF9FE7CDC983014751210307AC6296168948C3F64CE22F51F6E5424F936C846F1D01223B3D9864F4D955662103AC6AD514715BEC8D5DE1873B9BC873BB71773B51338B4D115F9938B6A029B7D152AEFFFFFFFF02C0175302000000001976A9146723D3398B384F0C0A8F717C100905F36E2ED7D488AC80969800000000001976A9148A033817801503863C9D6BD124D153F8407F2B4188AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("4ACC6214C74AF23AE430AFF165004D8E10DB9860B87FFB7E7E703AF97B872203010000009200483045022100BEB926DA7428FA009AC770576342EBD1960939E73584A5D0F3229B58C41E906F022017C0D143077906AFCCF30CAF21F5ECE0BB30E3F708FD4A17F9D9EF9FE7CDC983014751210307AC6296168948C3F64CE22F51F6E5424F936C846F1D01223B3D9864F4D955662103AC6AD514715BEC8D5DE1873B9BC873BB71773B51338B4D115F9938B6A029B7D152AEFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("80F0FA020000000017A9145C02C49641699863F909BF4BF3BE8398D2E383F187"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(177644L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("A9145C02C49641699863F909BF4BF3BE8398D2E383F187"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("00483045022100BEB926DA7428FA009AC770576342EBD1960939E73584A5D0F3229B58C41E906F022017C0D143077906AFCCF30CAF21F5ECE0BB30E3F708FD4A17F9D9EF9FE7CDC983014751210307AC6296168948C3F64CE22F51F6E5424F936C846F1D01223B3D9864F4D955662103AC6AD514715BEC8D5DE1873B9BC873BB71773B51338B4D115F9938B6A029B7D152AE"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }

    @Test
    public void should_verify_pay_to_script_hash_transaction_968A692AB98B1F275C635C76BE003AB1DB9740D0B62F338B270115342CA42F5B_1() {
        // Setup
        final TransactionInflater transactionInflater = new TransactionInflater();
        final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray("010000000120DA26875149D4F80F3C36ABC7F984720ED14F6396854AD1ABCA4FE72C88DC4F00000000FDFD0000483045022100939D7023833AAFFCB0DC2E6A0065316B90B7987BF3F4D99C5BC8811811782F34022064B5C3EF966E3312D615A06A3119D7599F12621BB92AC5CF1D971EF1AD9C8A65014730440220394151FB40EDD54326FA829EC571165A5E9168484293FD77E3713A35A701B43B02206B0FFF8488597790E2F6D62847CDE16DE3234439800F019423E7BEF881311862014C695221032C6AA78662CC43A3BB0F8F850D0C45E18D0A49C61EC69DB87E072C88D7A9B6E9210353581FD2FC745D17264AF8CB8CD507D82C9658962567218965E750590E41C41E21024FE45DD4749347D281FD5348F56E883EE3A00903AF899301AC47BA90F904854F53AEFFFFFFFF0330C11D00000000001976A914FD5E323C595B2614F47D6BE25BA079F081628C9B88AC80969800000000001976A91406F1B67078FC400A63D54C313CD6BB817E4760F088AC40787D01000000001976A9140231C76FF14600B49F7C1B734A69F169C7BA1BAC88AC00000000"));

        final TransactionInputInflater transactionInputInflater = new TransactionInputInflater();
        final TransactionInput transactionInput = transactionInputInflater.fromBytes(HexUtil.hexStringToByteArray("20DA26875149D4F80F3C36ABC7F984720ED14F6396854AD1ABCA4FE72C88DC4F00000000FDFD0000483045022100939D7023833AAFFCB0DC2E6A0065316B90B7987BF3F4D99C5BC8811811782F34022064B5C3EF966E3312D615A06A3119D7599F12621BB92AC5CF1D971EF1AD9C8A65014730440220394151FB40EDD54326FA829EC571165A5E9168484293FD77E3713A35A701B43B02206B0FFF8488597790E2F6D62847CDE16DE3234439800F019423E7BEF881311862014C695221032C6AA78662CC43A3BB0F8F850D0C45E18D0A49C61EC69DB87E072C88D7A9B6E9210353581FD2FC745D17264AF8CB8CD507D82C9658962567218965E750590E41C41E21024FE45DD4749347D281FD5348F56E883EE3A00903AF899301AC47BA90F904854F53AEFFFFFFFF"));

        final TransactionOutputInflater transactionOutputInflater = new TransactionOutputInflater();
        final TransactionOutput transactionOutput = transactionOutputInflater.fromBytes(1, HexUtil.hexStringToByteArray("409334020000000017A91493DD75558893D97C53005F6B63B9E4005401A93187"));

        final MutableContext context = new MutableContext();
        {
            context.setBlockHeight(177653L);
            context.setTransaction(transaction);

            context.setTransactionInput(transactionInput);
            context.setTransactionOutput(transactionOutput);
            context.setTransactionInputIndex(0);
        }

        final LockingScript lockingScript = new ImmutableLockingScript(HexUtil.hexStringToByteArray("A91493DD75558893D97C53005F6B63B9E4005401A93187"));
        final UnlockingScript unlockingScript = new ImmutableUnlockingScript(HexUtil.hexStringToByteArray("00483045022100939D7023833AAFFCB0DC2E6A0065316B90B7987BF3F4D99C5BC8811811782F34022064B5C3EF966E3312D615A06A3119D7599F12621BB92AC5CF1D971EF1AD9C8A65014730440220394151FB40EDD54326FA829EC571165A5E9168484293FD77E3713A35A701B43B02206B0FFF8488597790E2F6D62847CDE16DE3234439800F019423E7BEF881311862014C695221032C6AA78662CC43A3BB0F8F850D0C45E18D0A49C61EC69DB87E072C88D7A9B6E9210353581FD2FC745D17264AF8CB8CD507D82C9658962567218965E750590E41C41E21024FE45DD4749347D281FD5348F56E883EE3A00903AF899301AC47BA90F904854F53AE"));

        final ScriptRunner scriptRunner = new ScriptRunner();

        // Action
        final Boolean inputIsUnlocked = scriptRunner.runScript(lockingScript, unlockingScript, context);

        // Assert
        Assert.assertTrue(inputIsUnlocked);
    }
}