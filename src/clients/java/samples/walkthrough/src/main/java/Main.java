// section:imports
package com.tigerbeetle.samples;

import com.tigerbeetle.*;

public final class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Import ok!");
        // endsection:imports

        // section:client
        String replicaAddress = System.getenv("TB_ADDRESS");
        byte[] clusterID = UInt128.asBytes(0);
        String[] replicaAddresses = new String[] {replicaAddress == null ? "3000" : replicaAddress};
        try (var client = new Client(clusterID, replicaAddresses)) {
            // Use client
        }
        // endsection:client

        try (var client = new Client(clusterID, replicaAddresses)) {
            // section:create-accounts
            AccountBatch accounts = new AccountBatch(1);
            accounts.add();
            accounts.setId(137);
            accounts.setUserData128(UInt128.asBytes(java.util.UUID.randomUUID()));
            accounts.setUserData64(1234567890);
            accounts.setUserData32(42);
            accounts.setLedger(1);
            accounts.setCode(718);
            accounts.setFlags(0);

            CreateAccountResultBatch accountErrors = client.createAccounts(accounts);
            // endsection:create-accounts

            // section:account-flags
            accounts = new AccountBatch(3);

            // First account
            accounts.add();
            // Code to fill out fields for first account
            accounts.setFlags(AccountFlags.LINKED | AccountFlags.DEBITS_MUST_NOT_EXCEED_CREDITS);

            // Second account
            accounts.add();
            // Code to fill out fields for second account

            accountErrors = client.createAccounts(accounts);
            // endsection:account-flags

            // section:create-accounts-errors
            while (accountErrors.next()) {
                switch (accountErrors.getResult()) {
                    case Exists:
                        System.err.printf("Account at %d already exists.\n",
                                accountErrors.getIndex());
                        break;

                    default:
                        System.err.printf("Error creating account at %d: %s\n",
                                accountErrors.getIndex(), accountErrors.getResult());
                        break;
                }
            }
            // endsection:create-accounts-errors

            // section:lookup-accounts
            IdBatch ids = new IdBatch(2);
            ids.add(137);
            ids.add(138);
            accounts = client.lookupAccounts(ids);
            // endsection:lookup-accounts

            // section:create-transfers
            TransferBatch transfers = new TransferBatch(1);
            transfers.add();
            transfers.setId(1);
            transfers.setDebitAccountId(1);
            transfers.setCreditAccountId(2);
            transfers.setAmount(10);
            transfers.setUserData128(UInt128.asBytes(java.util.UUID.randomUUID()));
            transfers.setUserData64(1234567890);
            transfers.setUserData32(42);
            transfers.setTimeout(0);
            transfers.setLedger(1);
            transfers.setCode(1);
            transfers.setFlags(0);

            CreateTransferResultBatch transferErrors = client.createTransfers(transfers);
            // endsection:create-transfers

            // section:create-transfers-errors
            while (transferErrors.next()) {
                switch (transferErrors.getResult()) {
                    case ExceedsCredits:
                        System.err.printf("Transfer at %d exceeds credits.\n",
                                transferErrors.getIndex());
                        break;

                    default:
                        System.err.printf("Error creating transfer at %d: %s\n",
                                transferErrors.getIndex(), transferErrors.getResult());
                        break;
                }
            }
            // endsection:create-transfers-errors

            // section:no-batch
            var transferIds = new long[] {100, 101, 102};
            var debitIds = new long[] {1, 2, 3};
            var creditIds = new long[] {4, 5, 6};
            var amounts = new long[] {1000, 29, 11};
            for (int i = 0; i < transferIds.length; i++) {
                TransferBatch batch = new TransferBatch(1);
                batch.add();
                batch.setId(transferIds[i]);
                batch.setDebitAccountId(debitIds[i]);
                batch.setCreditAccountId(creditIds[i]);
                batch.setAmount(amounts[i]);

                CreateTransferResultBatch errors = client.createTransfers(batch);
                // Error handling omitted.
            }
            // endsection:no-batch

            // section:batch
            var BATCH_SIZE = 8190;
            for (int i = 0; i < transferIds.length; i += BATCH_SIZE) {
                TransferBatch batch = new TransferBatch(BATCH_SIZE);

                for (int j = 0; j < BATCH_SIZE && i + j < transferIds.length; j++) {
                    batch.add();
                    batch.setId(transferIds[i + j]);
                    batch.setDebitAccountId(debitIds[i + j]);
                    batch.setCreditAccountId(creditIds[i + j]);
                    batch.setAmount(amounts[i + j]);
                }

                CreateTransferResultBatch errors = client.createTransfers(batch);
                // Error handling omitted.
            }
            // endsection:batch

            // section:transfer-flags-link
            transfers = new TransferBatch(2);

            // First transfer
            transfers.add();
            // Code to fill out fields for first transfer
            transfers.setFlags(TransferFlags.LINKED);

            // Second transfer
            transfers.add();
            // Code to fill out fields for second transfer
            transferErrors = client.createTransfers(transfers);
            // endsection:transfer-flags-link

            // section:transfer-flags-post
            transfers = new TransferBatch(1);

            // First transfer
            transfers.add();
            // Code to fill out fields for first transfer
            transfers.setFlags(TransferFlags.POST_PENDING_TRANSFER);
            // Post the entire pending amount.
            transfers.setAmount(TransferBatch.AMOUNT_MAX);
            transferErrors = client.createTransfers(transfers);
            // endsection:transfer-flags-post

            // section:transfer-flags-void
            transfers = new TransferBatch(1);

            // First transfer
            transfers.add();
            // Code to fill out fields for first transfer
            transfers.setFlags(TransferFlags.VOID_PENDING_TRANSFER);
            transferErrors = client.createTransfers(transfers);
            // endsection:transfer-flags-void

            // section:lookup-transfers
            ids = new IdBatch(2);
            ids.add(1);
            ids.add(2);

            transfers = client.lookupTransfers(ids);
            // endsection:lookup-transfers

            // section:get-account-transfers
            AccountFilter filter = new AccountFilter();
            filter.setAccountId(2);
            filter.setUserData128(0); // No filter by UserData.
            filter.setUserData64(0);
            filter.setUserData32(0);
            filter.setCode(0); // No filter by Code.
            filter.setTimestampMin(0); // No filter by Timestamp.
            filter.setTimestampMax(0); // No filter by Timestamp.
            filter.setLimit(10); // Limit to ten transfers at most.
            filter.setDebits(true); // Include transfer from the debit side.
            filter.setCredits(true); // Include transfer from the credit side.
            filter.setReversed(true); // Sort by timestamp in reverse-chronological order.

            transfers = client.getAccountTransfers(filter);
            // endsection:get-account-transfers

            // section:get-account-balances
            filter = new AccountFilter();
            filter.setAccountId(2);
            filter.setUserData128(0); // No filter by UserData.
            filter.setUserData64(0);
            filter.setUserData32(0);
            filter.setCode(0); // No filter by Code.
            filter.setTimestampMin(0); // No filter by Timestamp.
            filter.setTimestampMax(0); // No filter by Timestamp.
            filter.setLimit(10); // Limit to ten balances at most.
            filter.setDebits(true); // Include transfer from the debit side.
            filter.setCredits(true); // Include transfer from the credit side.
            filter.setReversed(true); // Sort by timestamp in reverse-chronological order.

            AccountBalanceBatch account_balances = client.getAccountBalances(filter);
            // endsection:get-account-balances

            // section:query-accounts
            var query_filter = new QueryFilter();
            query_filter.setUserData128(1000); // Filter by UserData.
            query_filter.setUserData64(100);
            query_filter.setUserData32(10);
            query_filter.setCode(1); // Filter by Code.
            query_filter.setLedger(0); // No filter by Ledger.
            query_filter.setTimestampMin(0); // No filter by Timestamp.
            query_filter.setTimestampMax(0); // No filter by Timestamp.
            query_filter.setLimit(10); // Limit to ten balances at most.
            query_filter.setReversed(true); // Sort by timestamp in reverse-chronological order.

            AccountBatch query_accounts = client.queryAccounts(query_filter);
            // endsection:query-accounts

            // section:query-transfers
            query_filter = new QueryFilter();
            query_filter.setUserData128(1000); // Filter by UserData.
            query_filter.setUserData64(100);
            query_filter.setUserData32(10);
            query_filter.setCode(1); // Filter by Code.
            query_filter.setLedger(0); // No filter by Ledger.
            query_filter.setTimestampMin(0); // No filter by Timestamp.
            query_filter.setTimestampMax(0); // No filter by Timestamp.
            query_filter.setLimit(10); // Limit to ten balances at most.
            query_filter.setReversed(true); // Sort by timestamp in reverse-chronological order.

            TransferBatch query_transfers = client.queryTransfers(query_filter);
            // endsection:query-transfers

            // section:linked-events
            transfers = new TransferBatch(10);

            // An individual transfer (successful):
            transfers.add();
            transfers.setId(1);

            // A chain of 4 transfers (the last transfer in the chain closes the chain with
            // linked=false):
            transfers.add();
            transfers.setId(2); // Commit/rollback.
            transfers.setFlags(TransferFlags.LINKED);

            transfers.add();
            transfers.setId(3); // Commit/rollback.
            transfers.setFlags(TransferFlags.LINKED);

            transfers.add();
            transfers.setId(2); // Fail with exists
            transfers.setFlags(TransferFlags.LINKED);

            transfers.add();
            transfers.setId(4); // Fail without committing

            // An individual transfer (successful):
            // This should not see any effect from the failed chain above.
            transfers.add();
            transfers.setId(2);

            // A chain of 2 transfers (the first transfer fails the chain):
            transfers.add();
            transfers.setId(2);
            transfers.setFlags(TransferFlags.LINKED);

            transfers.add();
            transfers.setId(3);

            // A chain of 2 transfers (successful):
            transfers.add();
            transfers.setId(3);
            transfers.setFlags(TransferFlags.LINKED);

            transfers.add();
            transfers.setId(4);

            transferErrors = client.createTransfers(transfers);
            // endsection:linked-events

            // External source of time
            long historicalTimestamp = 0L;
            Object[] historicalAccounts = new Object[1];
            Object[] historicalTransfers = new Object[1];

            // section:imported-events
            // First, load and import all accounts with their timestamps from the historical source.
            accounts = new AccountBatch(historicalAccounts.length);
            for(int index = 0; index < historicalAccounts.length; index += 1) {
                accounts.add();

                // Set a unique and strictly increasing timestamp.
                historicalTimestamp += 1;
                accounts.setTimestamp(historicalTimestamp);
                // Set the account as `imported`.
                // To ensure atomicity, the entire batch (except the last event in the chain)
                // must be `linked`.
                if (index < historicalAccounts.length - 1) {
                    accounts.setFlags(AccountFlags.IMPORTED | AccountFlags.LINKED);
                } else {
                    accounts.setFlags(AccountFlags.IMPORTED);
                }

                // Populate the rest of the account:
                // accounts.setId(historicalAccounts[index].Id);
                // accounts.setCode(historicalAccounts[index].Code);
            }
            accountErrors = client.createAccounts(accounts);
            // Error handling omitted.

            // Then, load and import all transfers with their timestamps from the historical source.
            transfers = new TransferBatch(historicalTransfers.length);
            for(int index = 0; index < historicalTransfers.length; index += 1) {
                transfers.add();

                // Set a unique and strictly increasing timestamp.
                historicalTimestamp += 1;
                transfers.setTimestamp(historicalTimestamp);
                // Set the account as `imported`.
                // To ensure atomicity, the entire batch (except the last event in the chain)
                // must be `linked`.
                if (index < historicalTransfers.length - 1) {
                    transfers.setFlags(TransferFlags.IMPORTED | TransferFlags.LINKED);
                } else {
                    transfers.setFlags(TransferFlags.IMPORTED);
                }

                // Populate the rest of the transfer:
                // transfers.setId(historicalTransfers[index].Id);
                // transfers.setCode(historicalTransfers[index].Code);
            }
            transferErrors = client.createTransfers(transfers);
            // Error handling omitted.
            // Since it is a linked chain, in case of any error the entire batch is rolled back and can be retried
            // with the same historical timestamps without regressing the cluster timestamp.
            // endsection:imported-events
        }
        // section:imports
    }
}
// endsection:imports
