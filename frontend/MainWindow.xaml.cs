using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace SpringBootInterface;

/// <summary>
/// Interaction logic for MainWindow.xaml
/// </summary>
public partial class MainWindow : Window
{
    // We create client once.
    // Because on dipose it keeps dangling sockets.
    HttpClient client = new HttpClient();

    public MainWindow()
    {
        InitializeComponent();
    }

    private async void btnGetQuestion_Click(object sender, RoutedEventArgs e)
    {
        string url = "http://localhost:8080/questionnaire";
        HttpResponseMessage response = await client.GetAsync(url);

        //response.EnsureSuccessStatusCode(); // throws if not 2xx

        string content = await response.Content.ReadAsStringAsync();
        FixString(ref content);

        txtQuestion.Text = content;
    }

    private void FixString(ref string target)
    {
        string search = "&quot;";
        string replace = '"'.ToString();

        target = target.Replace(search, replace);

        search = "&#039;";
        replace = "'";

        target = target.Replace(search, replace);
    }
}