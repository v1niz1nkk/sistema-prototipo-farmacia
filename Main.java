import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Locale;

// =====================================================
//  SISTEMA DE CONTROLE DE FARMÁCIA - COMPLETO
//  Login + Permissões + Edição + Vendas + PIX (QR Code Atualizado)
// =====================================================

public class Main extends JFrame {

    private static final ArrayList<Cliente> clientes = new ArrayList<>();
    private static final ArrayList<Medicamento> medicamentos = new ArrayList<>();
    private static final ArrayList<Usuario> usuarios = new ArrayList<>();
    private static boolean dadosIniciaisCarregados = false;

    private static final DateTimeFormatter formatoData =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Matriz exata de 29x29 extraída da imagem qrcode_418262694_85949fe2d6d1f2cc0c4eb11eb498109f.png (https://q.me-qr.com/5xjik02t)
    private static final long[] QR_CODE_RICKROLL = {
            533087359L, 273840449L, 392051037L, 392079197L, 390839645L,
            274364737L, 534074751L, 655104L, 205549742L, 413555256L,
            282887138L, 497686618L, 399213224L, 119433383L, 340410073L,
            52462645L, 216005935L, 455441224L, 214922041L, 296403657L,
            408209399L, 602899L, 533505874L, 273534229L, 390755834L,
            390919577L, 391744340L, 274022859L, 533454996L
    };

    private Usuario usuarioLogado;

    private JTabbedPane abas;
    private DefaultTableModel modeloTabelaClientes;
    private DefaultTableModel modeloTabelaMedicamentos;
    private DefaultTableModel modeloTabelaUsuarios;
    private DefaultTableModel modeloTabelaCarrinho;

    // Campos de Cliente globais para permitir redirecionamento e edição
    private JTextField txtNomeCliente;
    private JTextField txtCpfCliente;
    private JTextField txtTelCliente;
    private Cliente clienteEmEdicao = null;
    private Medicamento medicamentoEmEdicao = null;

    // Estado da Venda atual
    private Cliente clienteVendaAtual = null;
    private final ArrayList<ItemVenda> carrinhoAtual = new ArrayList<>();
    private JLabel lblStatusClienteVenda;
    private JLabel lblTotalVenda;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (!dadosIniciaisCarregados) {
                carregarDadosIniciais();
                dadosIniciaisCarregados = true;
            }
            abrirTelaLogin();
        });
    }

    private static void carregarDadosIniciais() {
        usuarios.add(new Usuario("Farmacêutico Proprietário", "admin", "admin123", "ADMIN"));
        usuarios.add(new Usuario("Balconista Padrão", "balconista", "123", "BALCONISTA"));

        LocalDate hoje = LocalDate.now();
        clientes.add(new Cliente("Maria Souza", "11122233344", "(85) 99999-1111"));
        clientes.add(new Cliente("João Pereira", "55566677788", "(85) 98888-2222"));

        medicamentos.add(new Medicamento("M001", "Dipirona 500mg", "Dipirona", 8.50, 50, hoje.plusMonths(10), false));
        medicamentos.add(new Medicamento("M002", "Paracetamol 750mg", "Paracetamol", 12.90, 0, hoje.plusMonths(8), false));
        medicamentos.add(new Medicamento("C001", "Amoxicilina 500mg", "Amoxicilina", 32.00, 20, hoje.plusYears(1), true));
        medicamentos.add(new Medicamento("C002", "Clonazepam 2mg", "Clonazepam", 21.00, 10, hoje.minusDays(10), true));
    }

    // ================= TELA DE LOGIN =================
    public static void abrirTelaLogin() {
        JFrame frameLogin = new JFrame("Login - Sistema de Farmácia");
        frameLogin.setSize(400, 260);
        frameLogin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameLogin.setLocationRelativeTo(null);
        frameLogin.setLayout(new BorderLayout(10, 10));

        JPanel painel = new JPanel(new GridLayout(3, 2, 10, 15));
        painel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));

        JTextField txtLogin = new JTextField("admin");
        JPasswordField txtSenha = new JPasswordField("admin123");
        JButton btnEntrar = new JButton("Entrar no Sistema");

        painel.add(new JLabel("Usuário:"));
        painel.add(txtLogin);
        painel.add(new JLabel("Senha:"));
        painel.add(txtSenha);
        painel.add(new JLabel(""));
        painel.add(btnEntrar);

        JLabel lblDica = new JLabel("Dica: admin / admin123 (Farmacêutico) ou balconista / 123", SwingConstants.CENTER);
        lblDica.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblDica.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        btnEntrar.addActionListener(e -> {
            String login = txtLogin.getText().trim();
            String senha = new String(txtSenha.getPassword()).trim();

            Usuario autenticado = null;
            for (Usuario u : usuarios) {
                if (u.login.equals(login) && u.senha.equals(senha)) {
                    autenticado = u;
                    break;
                }
            }

            if (autenticado != null) {
                frameLogin.dispose();
                Main app = new Main(autenticado);
                app.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(frameLogin, "Usuário ou senha incorretos!", "Erro de Login", JOptionPane.ERROR_MESSAGE);
            }
        });

        frameLogin.add(painel, BorderLayout.CENTER);
        frameLogin.add(lblDica, BorderLayout.SOUTH);
        frameLogin.setVisible(true);
    }

    // ================= JANELA PRINCIPAL =================
    public Main(Usuario usuarioLogado) {
        super("Sistema de Controle de Farmácia");
        this.usuarioLogado = usuarioLogado;

        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel barraTopo = new JPanel(new BorderLayout());
        barraTopo.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        barraTopo.setBackground(new Color(230, 240, 250));

        String cargoTexto = isAdmin() ? "Farmacêutico Proprietário (Administrador)" : "Balconista (Acesso Restrito)";
        JLabel lblUsuario = new JLabel("Logado como: " + usuarioLogado.nome + "  |  Perfil: " + cargoTexto);
        lblUsuario.setFont(new Font("SansSerif", Font.BOLD, 13));

        JButton btnLogout = new JButton("Trocar Usuário (Logout)");
        btnLogout.addActionListener(e -> {
            dispose();
            abrirTelaLogin();
        });

        barraTopo.add(lblUsuario, BorderLayout.WEST);
        barraTopo.add(btnLogout, BorderLayout.EAST);

        abas = new JTabbedPane();
        abas.addTab("Medicamentos & Estoque", criarPainelMedicamentos());
        abas.addTab("Clientes", criarPainelClientes());
        abas.addTab("Vendas (PDV)", criarPainelVendas());

        if (isAdmin()) {
            abas.addTab("Adicionar Estoque", criarPainelAjusteEstoque());
            abas.addTab("Usuários / Funcionários", criarPainelUsuarios());
        }

        add(barraTopo, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);

        atualizarTabelaClientes();
        atualizarTabelaMedicamentos(medicamentos);
        if (isAdmin()) {
            atualizarTabelaUsuarios();
        }
    }

    private boolean isAdmin() {
        return usuarioLogado.cargo.equals("ADMIN");
    }

    // ================= ABA 1: MEDICAMENTOS & ESTOQUE =================
    private JPanel criarPainelMedicamentos() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(4, 4, 8, 8));
        String tituloForm = isAdmin() ? "Cadastrar / Editar Medicamento" : "Medicamentos (Somente Consulta para Balconista)";
        form.setBorder(BorderFactory.createTitledBorder(tituloForm));

        JTextField txtCodigo = new JTextField();
        JTextField txtNome = new JTextField();
        JTextField txtPrincipio = new JTextField();
        JTextField txtPreco = new JTextField();
        JTextField txtQtd = new JTextField();
        JTextField txtValidade = new JTextField();
        JCheckBox chkControlado = new JCheckBox("Controlado");
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnEditar = new JButton("Editar Selecionado na Tabela");

        form.add(new JLabel("Código:"));
        form.add(txtCodigo);
        form.add(new JLabel("Nome:"));
        form.add(txtNome);
        form.add(new JLabel("Princípio Ativo:"));
        form.add(txtPrincipio);
        form.add(new JLabel("Preço (ex: 12,50):"));
        form.add(txtPreco);
        form.add(new JLabel("Quantidade:"));
        form.add(txtQtd);
        form.add(new JLabel("Validade (dd/mm/aaaa):"));
        form.add(txtValidade);
        form.add(chkControlado);
        form.add(btnSalvar);
        form.add(new JLabel(""));
        form.add(btnEditar);

        if (!isAdmin()) {
            txtCodigo.setEnabled(false);
            txtNome.setEnabled(false);
            txtPrincipio.setEnabled(false);
            txtPreco.setEnabled(false);
            txtQtd.setEnabled(false);
            txtValidade.setEnabled(false);
            chkControlado.setEnabled(false);
            btnSalvar.setEnabled(false);
            btnEditar.setEnabled(false);
        }

        JPanel painelBusca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtBusca = new JTextField(20);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnVerTodos = new JButton("Ver Todos");

        painelBusca.add(new JLabel("Buscar (Nome, Código ou Princípio):"));
        painelBusca.add(txtBusca);
        painelBusca.add(btnBuscar);
        painelBusca.add(btnVerTodos);

        String[] colunas = {"CÓD", "NOME", "PRINC. ATIVO", "PREÇO", "QTD", "VALIDADE", "CTRL", "SITUAÇÃO"};
        modeloTabelaMedicamentos = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modeloTabelaMedicamentos);

        btnSalvar.addActionListener(e -> {
            try {
                String cod = txtCodigo.getText().trim().toUpperCase();
                String nome = txtNome.getText().trim();
                String principio = txtPrincipio.getText().trim();

                if (cod.isEmpty() || nome.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Código e Nome são obrigatórios.");
                    return;
                }

                double preco = Double.parseDouble(txtPreco.getText().trim().replace(',', '.'));
                int qtd = Integer.parseInt(txtQtd.getText().trim());
                LocalDate validade = LocalDate.parse(txtValidade.getText().trim(), formatoData);

                if (preco <= 0 || qtd < 0) {
                    JOptionPane.showMessageDialog(this, "Preço deve ser maior que zero e quantidade não negativa.");
                    return;
                }

                if (medicamentoEmEdicao == null) {
                    if (buscarMedicamentoPorCodigo(cod) != null) {
                        JOptionPane.showMessageDialog(this, "Já existe um medicamento com esse código.");
                        return;
                    }
                    medicamentos.add(new Medicamento(cod, nome, principio, preco, qtd, validade, chkControlado.isSelected()));
                    JOptionPane.showMessageDialog(this, "Medicamento cadastrado com sucesso!");
                } else {
                    medicamentoEmEdicao.nome = nome;
                    medicamentoEmEdicao.principioAtivo = principio;
                    medicamentoEmEdicao.preco = preco;
                    medicamentoEmEdicao.quantidade = qtd;
                    medicamentoEmEdicao.validade = validade;
                    medicamentoEmEdicao.controlado = chkControlado.isSelected();
                    medicamentoEmEdicao = null;
                    txtCodigo.setEditable(true);
                    btnSalvar.setText("Cadastrar");
                    JOptionPane.showMessageDialog(this, "Medicamento modificado com sucesso!");
                }

                atualizarTabelaMedicamentos(medicamentos);
                txtCodigo.setText(""); txtNome.setText(""); txtPrincipio.setText("");
                txtPreco.setText(""); txtQtd.setText(""); txtValidade.setText("");
                chkControlado.setSelected(false);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Digite valores numéricos válidos para preço e quantidade.");
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Data inválida. Use o formato dd/mm/aaaa.");
            }
        });

        btnEditar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha == -1) {
                JOptionPane.showMessageDialog(this, "Selecione um medicamento na tabela para editar.");
                return;
            }
            String cod = (String) modeloTabelaMedicamentos.getValueAt(linha, 0);
            Medicamento m = buscarMedicamentoPorCodigo(cod);
            if (m != null) {
                medicamentoEmEdicao = m;
                txtCodigo.setText(m.codigo);
                txtCodigo.setEditable(false);
                txtNome.setText(m.nome);
                txtPrincipio.setText(m.principioAtivo);
                txtPreco.setText(String.format(Locale.US, "%.2f", m.preco).replace('.', ','));
                txtQtd.setText(String.valueOf(m.quantidade));
                txtValidade.setText(m.validade.format(formatoData));
                chkControlado.setSelected(m.controlado);
                btnSalvar.setText("Salvar Alterações");
            }
        });

        btnBuscar.addActionListener(e -> {
            String termo = txtBusca.getText().trim().toLowerCase();
            ArrayList<Medicamento> achados = new ArrayList<>();
            for (Medicamento m : medicamentos) {
                if (m.nome.toLowerCase().contains(termo)
                        || m.principioAtivo.toLowerCase().contains(termo)
                        || m.codigo.toLowerCase().contains(termo)) {
                    achados.add(m);
                }
            }
            atualizarTabelaMedicamentos(achados);
        });

        btnVerTodos.addActionListener(e -> {
            txtBusca.setText("");
            atualizarTabelaMedicamentos(medicamentos);
        });

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.NORTH);
        topo.add(painelBusca, BorderLayout.SOUTH);

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    // ================= ABA 2: CLIENTES =================
    private JPanel criarPainelClientes() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Cadastrar / Gerenciar Clientes"));

        txtNomeCliente = new JTextField();
        txtCpfCliente = new JTextField();
        txtTelCliente = new JTextField();
        JButton btnSalvarCliente = new JButton("Cadastrar Cliente");
        JButton btnEditarCliente = new JButton("Editar Selecionado");

        if (!isAdmin()) {
            btnEditarCliente.setEnabled(false);
            btnEditarCliente.setToolTipText("Apenas o Farmacêutico Administrador pode modificar clientes.");
        }

        form.add(new JLabel("Nome:")); form.add(txtNomeCliente);
        form.add(new JLabel("CPF (11 dígitos):")); form.add(txtCpfCliente);
        form.add(new JLabel("Telefone:")); form.add(txtTelCliente);
        form.add(btnEditarCliente); form.add(btnSalvarCliente);

        String[] colunas = {"NOME", "CPF", "TELEFONE"};
        modeloTabelaClientes = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modeloTabelaClientes);

        btnSalvarCliente.addActionListener(e -> {
            String nome = txtNomeCliente.getText().trim();
            String cpf = txtCpfCliente.getText().replaceAll("[^0-9]", "");
            String tel = txtTelCliente.getText().trim();

            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O nome é obrigatório.");
                return;
            }
            if (cpf.length() != 11) {
                JOptionPane.showMessageDialog(this, "O CPF precisa ter 11 números.");
                return;
            }

            if (clienteEmEdicao == null) {
                if (buscarClientePorCpf(cpf) != null) {
                    JOptionPane.showMessageDialog(this, "Já existe um cliente com esse CPF.");
                    return;
                }
                clientes.add(new Cliente(nome, cpf, tel));
                JOptionPane.showMessageDialog(this, "Cliente cadastrado com sucesso!");
            } else {
                Cliente existente = buscarClientePorCpf(cpf);
                if (existente != null && existente != clienteEmEdicao) {
                    JOptionPane.showMessageDialog(this, "Este CPF já pertence a outro cliente.");
                    return;
                }
                clienteEmEdicao.nome = nome;
                clienteEmEdicao.cpf = cpf;
                clienteEmEdicao.telefone = tel;
                clienteEmEdicao = null;
                btnSalvarCliente.setText("Cadastrar Cliente");
                JOptionPane.showMessageDialog(this, "Cadastro do cliente atualizado com sucesso!");
            }

            atualizarTabelaClientes();
            txtNomeCliente.setText(""); txtCpfCliente.setText(""); txtTelCliente.setText("");
        });

        btnEditarCliente.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha == -1) {
                JOptionPane.showMessageDialog(this, "Selecione um cliente na tabela para editar.");
                return;
            }
            Cliente c = clientes.get(linha);
            clienteEmEdicao = c;
            txtNomeCliente.setText(c.nome);
            txtCpfCliente.setText(c.cpf);
            txtTelCliente.setText(c.telefone);
            btnSalvarCliente.setText("Salvar Alterações");
        });

        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    // ================= ABA 3: SISTEMA DE VENDAS (PDV) =================
    private JPanel criarPainelVendas() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel painelCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        painelCliente.setBorder(BorderFactory.createTitledBorder("1. Identificação do Cliente"));

        JTextField txtCpfVenda = new JTextField(14);
        JButton btnVerificarCpf = new JButton("Verificar CPF");
        lblStatusClienteVenda = new JLabel("Nenhum cliente selecionado.");
        lblStatusClienteVenda.setFont(new Font("SansSerif", Font.BOLD, 12));

        painelCliente.add(new JLabel("CPF do Cliente:"));
        painelCliente.add(txtCpfVenda);
        painelCliente.add(btnVerificarCpf);
        painelCliente.add(lblStatusClienteVenda);

        JPanel painelItem = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        painelItem.setBorder(BorderFactory.createTitledBorder("2. Adicionar Medicamento à Venda"));

        JTextField txtCodMedVenda = new JTextField(10);
        JTextField txtQtdVenda = new JTextField("1", 5);
        JButton btnAddItem = new JButton("Adicionar ao Carrinho");

        painelItem.add(new JLabel("Código do Medicamento:"));
        painelItem.add(txtCodMedVenda);
        painelItem.add(new JLabel("Quantidade:"));
        painelItem.add(txtQtdVenda);
        painelItem.add(btnAddItem);

        JPanel topoVendas = new JPanel(new GridLayout(2, 1, 5, 5));
        topoVendas.add(painelCliente);
        topoVendas.add(painelItem);

        String[] colunas = {"CÓDIGO", "MEDICAMENTO", "QTD", "PREÇO UNIT.", "SUBTOTAL"};
        modeloTabelaCarrinho = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaCarrinho = new JTable(modeloTabelaCarrinho);

        JPanel rodapeVendas = new JPanel(new BorderLayout());
        lblTotalVenda = new JLabel("Total da Venda: R$ 0,00");
        lblTotalVenda.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel botoesRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLimparCarrinho = new JButton("Cancelar / Limpar");
        JButton btnFinalizarVenda = new JButton("Finalizar Venda (Escolher Pagamento)");

        botoesRodape.add(btnLimparCarrinho);
        botoesRodape.add(btnFinalizarVenda);

        rodapeVendas.add(lblTotalVenda, BorderLayout.WEST);
        rodapeVendas.add(botoesRodape, BorderLayout.EAST);

        btnVerificarCpf.addActionListener(e -> {
            String cpfDigitado = txtCpfVenda.getText().replaceAll("[^0-9]", "");
            if (cpfDigitado.length() != 11) {
                JOptionPane.showMessageDialog(this, "Digite um CPF válido com 11 números.");
                return;
            }

            Cliente encontrado = buscarClientePorCpf(cpfDigitado);
            if (encontrado != null) {
                clienteVendaAtual = encontrado;
                lblStatusClienteVenda.setText("Cliente: " + encontrado.nome + " | Tel: " + encontrado.telefone);
                lblStatusClienteVenda.setForeground(new Color(0, 128, 0));
            } else {
                clienteVendaAtual = null;
                lblStatusClienteVenda.setText("CPF não cadastrado.");
                lblStatusClienteVenda.setForeground(Color.RED);

                int resposta = JOptionPane.showConfirmDialog(
                        this,
                        "CPF não encontrado, deseja cadastrar um novo cliente?",
                        "Cliente Não Encontrado",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (resposta == JOptionPane.YES_OPTION) {
                    abas.setSelectedIndex(1);
                    clienteEmEdicao = null;
                    txtCpfCliente.setText(cpfDigitado);
                    txtNomeCliente.setText("");
                    txtTelCliente.setText("");
                    txtNomeCliente.requestFocus();
                }
            }
        });

        btnAddItem.addActionListener(e -> {
            if (clienteVendaAtual == null) {
                JOptionPane.showMessageDialog(this, "Primeiro identifique um cliente válido pelo CPF!");
                return;
            }

            String cod = txtCodMedVenda.getText().trim().toUpperCase();
            Medicamento med = buscarMedicamentoPorCodigo(cod);
            if (med == null) {
                JOptionPane.showMessageDialog(this, "Medicamento não encontrado com o código: " + cod);
                return;
            }

            if (med.situacao().equals("VENCIDO")) {
                JOptionPane.showMessageDialog(this, "Este medicamento está VENCIDO e não pode ser vendido!", "Bloqueio", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int qtdDesejada = Integer.parseInt(txtQtdVenda.getText().trim());
                if (qtdDesejada <= 0) {
                    JOptionPane.showMessageDialog(this, "A quantidade deve ser maior que zero.");
                    return;
                }

                int jaNoCarrinho = 0;
                for (ItemVenda item : carrinhoAtual) {
                    if (item.medicamento.codigo.equals(med.codigo)) {
                        jaNoCarrinho += item.quantidade;
                    }
                }

                if (qtdDesejada + jaNoCarrinho > med.quantidade) {
                    JOptionPane.showMessageDialog(this, "Estoque insuficiente! Disponível: " + med.quantidade + " unidades.");
                    return;
                }

                carrinhoAtual.add(new ItemVenda(med, qtdDesejada));
                atualizarTabelaCarrinho();
                txtCodMedVenda.setText("");
                txtQtdVenda.setText("1");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantidade inválida.");
            }
        });

        btnLimparCarrinho.addActionListener(e -> {
            carrinhoAtual.clear();
            clienteVendaAtual = null;
            txtCpfVenda.setText("");
            lblStatusClienteVenda.setText("Nenhum cliente selecionado.");
            lblStatusClienteVenda.setForeground(Color.BLACK);
            atualizarTabelaCarrinho();
        });

        btnFinalizarVenda.addActionListener(e -> {
            if (clienteVendaAtual == null) {
                JOptionPane.showMessageDialog(this, "Identifique o cliente antes de finalizar a venda.");
                return;
            }
            if (carrinhoAtual.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O carrinho está vazio.");
                return;
            }

            double total = 0;
            for (ItemVenda item : carrinhoAtual) {
                total += item.getSubtotal();
            }
            String totalFormatado = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", total);

            String[] opcoesPagamento = {"PIX (QR Code)", "Dinheiro em Espécie", "Cancelar"};
            int escolha = JOptionPane.showOptionDialog(
                    this,
                    "Cliente: " + clienteVendaAtual.nome + "\nTotal a Pagar: " + totalFormatado + "\n\nSelecione a forma de pagamento:",
                    "Forma de Pagamento",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoesPagamento,
                    opcoesPagamento[0]
            );

            boolean pagamentoConfirmado = false;
            String resumoPagamento = "";

            if (escolha == 0) {
                pagamentoConfirmado = mostrarModalPagamentoPix(totalFormatado);
                resumoPagamento = "Forma de Pagamento: PIX";
            } else if (escolha == 1) {
                String resultadoDinheiro = processarPagamentoDinheiro(total, totalFormatado);
                if (resultadoDinheiro != null) {
                    pagamentoConfirmado = true;
                    resumoPagamento = resultadoDinheiro;
                }
            }

            if (pagamentoConfirmado) {
                for (ItemVenda item : carrinhoAtual) {
                    item.medicamento.quantidade -= item.quantidade;
                }

                atualizarTabelaMedicamentos(medicamentos);

                JOptionPane.showMessageDialog(this,
                        "Venda concluída com sucesso!\n"
                                + "Cliente: " + clienteVendaAtual.nome + "\n"
                                + "Total da Venda: " + totalFormatado + "\n"
                                + resumoPagamento);

                carrinhoAtual.clear();
                clienteVendaAtual = null;
                txtCpfVenda.setText("");
                lblStatusClienteVenda.setText("Nenhum cliente selecionado.");
                lblStatusClienteVenda.setForeground(Color.BLACK);
                atualizarTabelaCarrinho();
            }
        });

        painel.add(topoVendas, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaCarrinho), BorderLayout.CENTER);
        painel.add(rodapeVendas, BorderLayout.SOUTH);
        return painel;
    }

    // ================= MODAL DE PAGAMENTO PIX (EXIBE A NOVA IMAGEM) =================
    private boolean mostrarModalPagamentoPix(String totalFormatado) {
        JDialog dialogPix = new JDialog(this, "Pagamento via PIX", true);
        dialogPix.setSize(400, 500);
        dialogPix.setLocationRelativeTo(this);
        dialogPix.setLayout(new BorderLayout(10, 10));

        JPanel topo = new JPanel(new GridLayout(3, 1, 2, 2));
        topo.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        JLabel lblTitulo = new JLabel("Escaneie o QR Code para pagar", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel lblValor = new JLabel("Valor: " + totalFormatado, SwingConstants.CENTER);
        lblValor.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblValor.setForeground(new Color(0, 128, 0));

        JLabel lblSub = new JLabel("Aponte a câmera do seu celular para o código abaixo:", SwingConstants.CENTER);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));

        topo.add(lblTitulo);
        topo.add(lblValor);
        topo.add(lblSub);

        JComponent componenteQr;
        File arquivoImg = new File("qrcode_418262694_85949fe2d6d1f2cc0c4eb11eb498109f.png");
        if (!arquivoImg.exists()) {
            arquivoImg = new File("qrcode.png");
        }

        if (arquivoImg.exists()) {
            ImageIcon iconeOriginal = new ImageIcon(arquivoImg.getAbsolutePath());
            Image imgRedimensionada = iconeOriginal.getImage().getScaledInstance(280, 280, Image.SCALE_SMOOTH);
            JLabel lblImagem = new JLabel(new ImageIcon(imgRedimensionada));
            lblImagem.setHorizontalAlignment(SwingConstants.CENTER);
            componenteQr = lblImagem;
        } else {
            JPanel painelQr = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    int tamanhoMatriz = 29;
                    int margemModulos = 2;
                    int totalModulos = tamanhoMatriz + (margemModulos * 2);

                    int menorLado = Math.min(getWidth(), getHeight());
                    int escala = Math.max(1, menorLado / totalModulos);

                    int tamanhoDesenho = totalModulos * escala;
                    int offsetX = (getWidth() - tamanhoDesenho) / 2;
                    int offsetY = (getHeight() - tamanhoDesenho) / 2;

                    g2.setColor(Color.WHITE);
                    g2.fillRect(offsetX, offsetY, tamanhoDesenho, tamanhoDesenho);

                    g2.setColor(Color.BLACK);
                    for (int linha = 0; linha < tamanhoMatriz; linha++) {
                        long bitsLinha = QR_CODE_RICKROLL[linha];
                        for (int col = 0; col < tamanhoMatriz; col++) {
                            boolean moduloPreto = ((bitsLinha >> (28 - col)) & 1L) == 1L;
                            if (moduloPreto) {
                                int x = offsetX + (col + margemModulos) * escala;
                                int y = offsetY + (linha + margemModulos) * escala;
                                g2.fillRect(x, y, escala, escala);
                            }
                        }
                    }
                }
            };
            painelQr.setPreferredSize(new Dimension(280, 280));
            componenteQr = painelQr;
        }

        final boolean[] confirmado = {false};

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        JButton btnConfirmar = new JButton("Confirmar Recebimento do PIX");
        btnConfirmar.setFont(new Font("SansSerif", Font.BOLD, 12));
        JButton btnCancelar = new JButton("Voltar");

        btnConfirmar.addActionListener(e -> {
            confirmado[0] = true;
            dialogPix.dispose();
        });

        btnCancelar.addActionListener(e -> dialogPix.dispose());

        rodape.add(btnCancelar);
        rodape.add(btnConfirmar);

        dialogPix.add(topo, BorderLayout.NORTH);
        dialogPix.add(componenteQr, BorderLayout.CENTER);
        dialogPix.add(rodape, BorderLayout.SOUTH);

        dialogPix.setVisible(true);
        return confirmado[0];
    }

    // ================= PROCESSAMENTO DE DINHEIRO EM ESPÉCIE =================
    private String processarPagamentoDinheiro(double total, String totalFormatado) {
        while (true) {
            String entrada = JOptionPane.showInputDialog(
                    this,
                    "Total a Pagar: " + totalFormatado + "\nDigite o valor recebido em dinheiro (R$):",
                    "Pagamento em Espécie",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (entrada == null) {
                return null;
            }

            try {
                double valorRecebido = Double.parseDouble(entrada.trim().replace(',', '.'));
                if (valorRecebido < total) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Valor insuficiente! O valor recebido deve ser igual ou maior que " + totalFormatado + ".",
                            "Valor Insuficiente",
                            JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    double troco = valorRecebido - total;
                    String recebidoStr = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valorRecebido);
                    String trocoStr = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", troco);
                    return "Forma de Pagamento: Dinheiro em Espécie\nValor Recebido: " + recebidoStr + "\nTroco: " + trocoStr;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Digite um valor numérico válido (ex: 50,00).", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================= ABA 4: ADICIONAR ESTOQUE (ADMIN) =================
    private JPanel criarPainelAjusteEstoque() {
        JPanel painel = new JPanel(new GridBagLayout());
        JPanel caixa = new JPanel(new GridLayout(3, 2, 10, 10));
        caixa.setBorder(BorderFactory.createTitledBorder("Adicionar Quantidade ao Estoque"));

        JTextField txtCodigo = new JTextField(15);
        JTextField txtQtd = new JTextField(15);
        JButton btnAdicionar = new JButton("Adicionar Estoque");

        caixa.add(new JLabel("Código do Medicamento:")); caixa.add(txtCodigo);
        caixa.add(new JLabel("Quantidade a Adicionar:")); caixa.add(txtQtd);
        caixa.add(new JLabel("")); caixa.add(btnAdicionar);

        btnAdicionar.addActionListener(e -> {
            Medicamento m = buscarMedicamentoPorCodigo(txtCodigo.getText().trim());
            if (m == null) {
                JOptionPane.showMessageDialog(this, "Medicamento não encontrado.");
                return;
            }
            try {
                int qtd = Integer.parseInt(txtQtd.getText().trim());
                if (qtd <= 0) {
                    JOptionPane.showMessageDialog(this, "A quantidade deve ser maior que zero.");
                    return;
                }
                m.quantidade += qtd;
                atualizarTabelaMedicamentos(medicamentos);
                txtCodigo.setText(""); txtQtd.setText("");
                JOptionPane.showMessageDialog(this, m.nome + ": agora tem " + m.quantidade + " unidades.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Digite um número inteiro válido.");
            }
        });

        painel.add(caixa);
        return painel;
    }

    // ================= ABA 5: GERENCIAR USUÁRIOS (ADMIN) =================
    private JPanel criarPainelUsuarios() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Criar Nova Conta Local (Balconista ou Farmacêutico)"));

        JTextField txtNome = new JTextField();
        JTextField txtLogin = new JTextField();
        JPasswordField txtSenha = new JPasswordField();
        JComboBox<String> cbCargo = new JComboBox<>(new String[]{"BALCONISTA", "ADMIN"});
        JButton btnCriar = new JButton("Criar Conta");

        form.add(new JLabel("Nome do Funcionário:")); form.add(txtNome);
        form.add(new JLabel("Usuário (Login):")); form.add(txtLogin);
        form.add(new JLabel("Senha:")); form.add(txtSenha);
        form.add(new JLabel("Perfil de Acesso:")); form.add(cbCargo);
        form.add(new JLabel("")); form.add(new JLabel(""));
        form.add(new JLabel("")); form.add(btnCriar);

        String[] colunas = {"NOME", "LOGIN", "CARGO / PERMISSÃO"};
        modeloTabelaUsuarios = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modeloTabelaUsuarios);

        btnCriar.addActionListener(e -> {
            String nome = txtNome.getText().trim();
            String login = txtLogin.getText().trim();
            String senha = new String(txtSenha.getPassword()).trim();
            String cargo = (String) cbCargo.getSelectedItem();

            if (nome.isEmpty() || login.isEmpty() || senha.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha todos os campos para criar a conta.");
                return;
            }

            for (Usuario u : usuarios) {
                if (u.login.equalsIgnoreCase(login)) {
                    JOptionPane.showMessageDialog(this, "Já existe um usuário com esse login.");
                    return;
                }
            }

            usuarios.add(new Usuario(nome, login, senha, cargo));
            atualizarTabelaUsuarios();
            txtNome.setText(""); txtLogin.setText(""); txtSenha.setText("");
            JOptionPane.showMessageDialog(this, "Conta criada com sucesso! Use o botão 'Trocar Usuário' no topo para testar.");
        });

        painel.add(form, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    // ================= ATUALIZAÇÃO DE TABELAS E BUSCAS =================
    private void atualizarTabelaClientes() {
        modeloTabelaClientes.setRowCount(0);
        for (Cliente c : clientes) {
            String cpfFormatado = c.cpf.substring(0, 3) + "." + c.cpf.substring(3, 6) + "."
                    + c.cpf.substring(6, 9) + "-" + c.cpf.substring(9);
            modeloTabelaClientes.addRow(new Object[]{c.nome, cpfFormatado, c.telefone});
        }
    }

    private void atualizarTabelaMedicamentos(ArrayList<Medicamento> lista) {
        modeloTabelaMedicamentos.setRowCount(0);
        for (Medicamento m : lista) {
            String preco = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", m.preco);
            modeloTabelaMedicamentos.addRow(new Object[]{
                    m.codigo, m.nome, m.principioAtivo, preco, m.quantidade,
                    m.validade.format(formatoData), m.controlado ? "SIM" : "não", m.situacao()
            });
        }
    }

    private void atualizarTabelaUsuarios() {
        if (modeloTabelaUsuarios == null) return;
        modeloTabelaUsuarios.setRowCount(0);
        for (Usuario u : usuarios) {
            String descricaoCargo = u.cargo.equals("ADMIN") ? "Farmacêutico (Administrador)" : "Balconista";
            modeloTabelaUsuarios.addRow(new Object[]{u.nome, u.login, descricaoCargo});
        }
    }

    private void atualizarTabelaCarrinho() {
        modeloTabelaCarrinho.setRowCount(0);
        double total = 0;
        for (ItemVenda item : carrinhoAtual) {
            String unitario = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", item.medicamento.preco);
            String sub = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", item.getSubtotal());
            modeloTabelaCarrinho.addRow(new Object[]{
                    item.medicamento.codigo, item.medicamento.nome, item.quantidade, unitario, sub
            });
            total += item.getSubtotal();
        }
        lblTotalVenda.setText(String.format(Locale.forLanguageTag("pt-BR"), "Total da Venda: R$ %.2f", total));
    }

    private static Cliente buscarClientePorCpf(String cpf) {
        for (Cliente c : clientes) {
            if (c.cpf.equals(cpf)) return c;
        }
        return null;
    }

    private static Medicamento buscarMedicamentoPorCodigo(String codigo) {
        for (Medicamento m : medicamentos) {
            if (m.codigo.equalsIgnoreCase(codigo.trim())) return m;
        }
        return null;
    }
}

// ---------- Classes de Modelo ----------
class Usuario {
    String nome;
    String login;
    String senha;
    String cargo;

    Usuario(String nome, String login, String senha, String cargo) {
        this.nome = nome;
        this.login = login;
        this.senha = senha;
        this.cargo = cargo;
    }
}

class ItemVenda {
    Medicamento medicamento;
    int quantidade;

    ItemVenda(Medicamento medicamento, int quantidade) {
        this.medicamento = medicamento;
        this.quantidade = quantidade;
    }

    double getSubtotal() {
        return medicamento.preco * quantidade;
    }
}

class Cliente {
    String nome;
    String cpf;
    String telefone;

    Cliente(String nome, String cpf, String telefone) {
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
    }
}

class Medicamento {
    String codigo;
    String nome;
    String principioAtivo;
    double preco;
    int quantidade;
    LocalDate validade;
    boolean controlado;

    Medicamento(String codigo, String nome, String principioAtivo, double preco,
                int quantidade, LocalDate validade, boolean controlado) {
        this.codigo = codigo;
        this.nome = nome;
        this.principioAtivo = principioAtivo;
        this.preco = preco;
        this.quantidade = quantidade;
        this.validade = validade;
        this.controlado = controlado;
    }

    String situacao() {
        if (validade.isBefore(LocalDate.now())) return "VENCIDO";
        if (quantidade == 0) return "SEM ESTOQUE";
        return "OK";
    }
}