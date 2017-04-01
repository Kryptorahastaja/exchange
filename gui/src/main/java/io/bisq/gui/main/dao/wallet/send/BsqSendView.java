/*
 * This file is part of bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.gui.main.dao.wallet.send;

import com.google.common.util.concurrent.FutureCallback;
import io.bisq.common.app.DevEnv;
import io.bisq.common.locale.Res;
import io.bisq.core.btc.InsufficientFundsException;
import io.bisq.core.btc.exceptions.TransactionVerificationException;
import io.bisq.core.btc.exceptions.WalletException;
import io.bisq.core.btc.wallet.BsqWalletService;
import io.bisq.core.btc.wallet.BtcWalletService;
import io.bisq.core.provider.fee.FeeService;
import io.bisq.core.util.CoinUtil;
import io.bisq.gui.common.view.ActivatableView;
import io.bisq.gui.common.view.FxmlView;
import io.bisq.gui.components.InputTextField;
import io.bisq.gui.main.dao.wallet.BalanceUtil;
import io.bisq.gui.main.overlays.popups.Popup;
import io.bisq.gui.util.BSFormatter;
import io.bisq.gui.util.BsqFormatter;
import io.bisq.gui.util.Layout;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static io.bisq.gui.util.FormBuilder.*;

@FxmlView
public class BsqSendView extends ActivatableView<GridPane, Void> {


    private TextField balanceTextField;

    private final BsqWalletService bsqWalletService;
    private final BtcWalletService btcWalletService;
    private final FeeService feeService;
    private final BSFormatter bsqFormatter;
    private final BSFormatter btcFormatter;
    private final BalanceUtil balanceUtil;

    private int gridRow = 0;
    private InputTextField amountInputTextField;
    private Button sendButton;
    private InputTextField receiversAddressInputTextField;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, lifecycle
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    private BsqSendView(BsqWalletService bsqWalletService, BtcWalletService btcWalletService, FeeService feeService, BsqFormatter bsqFormatter, BSFormatter btcFormatter, BalanceUtil balanceUtil) {
        this.bsqWalletService = bsqWalletService;
        this.btcWalletService = btcWalletService;
        this.feeService = feeService;

        this.bsqFormatter = bsqFormatter;
        this.btcFormatter = btcFormatter;
        this.balanceUtil = balanceUtil;
    }

    @Override
    public void initialize() {
        addTitledGroupBg(root, gridRow, 1, Res.get("shared.balance"));
        balanceTextField = addLabelTextField(root, gridRow, Res.get("shared.bsqBalance"), Layout.FIRST_ROW_DISTANCE).second;
        balanceUtil.setBalanceTextField(balanceTextField);
        balanceUtil.initialize();

        addTitledGroupBg(root, ++gridRow, 3, Res.get("dao.wallet.send.sendFunds"), Layout.GROUP_DISTANCE);
        amountInputTextField = addLabelInputTextField(root, gridRow, Res.get("dao.wallet.send.amount"), Layout.FIRST_ROW_AND_GROUP_DISTANCE).second;
        amountInputTextField.setPromptText(Res.get("dao.wallet.send.setAmount", Transaction.MIN_NONDUST_OUTPUT.value));

        receiversAddressInputTextField = addLabelInputTextField(root, ++gridRow,
                Res.get("dao.wallet.send.receiverAddress")).second;
        receiversAddressInputTextField.setPromptText(Res.get("dao.wallet.send.setDestinationAddress"));

        sendButton = addButtonAfterGroup(root, ++gridRow, Res.get("dao.wallet.send.send"));

        if (DevEnv.DEV_MODE) {
            amountInputTextField.setText("2.730"); // 2730 is dust limit
            receiversAddressInputTextField.setText("mpaZiEh8gSr4LcH11FrLdRY57aArt88qtg");
        }

        sendButton.setOnAction((event) -> {
            String receiversAddressString = receiversAddressInputTextField.getText();
            Coin receiverAmount = bsqFormatter.parseToCoin(amountInputTextField.getText());
            try {
                Transaction preparedSendTx = bsqWalletService.getPreparedSendTx(receiversAddressString, receiverAmount);
                Transaction txWithBtcFee = btcWalletService.completePreparedSendBsqTx(preparedSendTx, true);
                Transaction signedTx = bsqWalletService.signTx(txWithBtcFee);
                Coin miningFee = signedTx.getFee();
                int txSize = signedTx.bitcoinSerialize().length;
                new Popup().headLine(Res.get("dao.wallet.send.sendFunds.headline"))
                        .confirmation(Res.get("dao.wallet.send.sendFunds.details",
                                bsqFormatter.formatCoinWithCode(receiverAmount),
                                receiversAddressString,
                                btcFormatter.formatCoinWithCode(miningFee),
                                CoinUtil.getFeePerByte(miningFee, txSize),
                                txSize / 1000d,
                                bsqFormatter.formatCoinWithCode(receiverAmount)))
                        .actionButtonText(Res.get("shared.yes"))
                        .onAction(() -> {
                            try {
                                bsqWalletService.commitTx(txWithBtcFee);
                                // We need to create another instance, otherwise the tx would trigger an invalid state exception 
                                // if it gets committed 2 times 
                                btcWalletService.commitTx(btcWalletService.getClonedTransaction(txWithBtcFee));
                                bsqWalletService.broadcastTx(signedTx, new FutureCallback<Transaction>() {
                                    @Override
                                    public void onSuccess(@Nullable Transaction transaction) {
                                        if (transaction != null) {
                                            log.error("Successfully sent tx with id " + transaction.getHashAsString());
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NotNull Throwable t) {
                                        log.error(t.toString());
                                        new Popup<>().warning(t.toString());
                                    }
                                });
                            } catch (WalletException | TransactionVerificationException e) {
                                log.error(e.toString());
                                e.printStackTrace();
                                new Popup<>().warning(e.toString());
                            }
                        })
                        .closeButtonText(Res.get("shared.cancel"))
                        .show();
            } catch (AddressFormatException | InsufficientFundsException |
                    TransactionVerificationException | WalletException | InsufficientMoneyException e) {
                log.error(e.toString());
                e.printStackTrace();
                new Popup<>().warning(e.toString()).show();
            }
        });
    }

    @Override
    protected void activate() {
        balanceUtil.activate();
    }

    @Override
    protected void deactivate() {
        balanceUtil.deactivate();
    }
}
