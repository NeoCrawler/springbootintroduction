using Newtonsoft.Json;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;

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
        // Hide result box.
        txtResult.Visibility = Visibility.Hidden;

        string url = "http://localhost:8080/questionnaire";
        HttpResponseMessage response = await client.GetAsync(url);
        
        var content = await response.Content.ReadAsStringAsync();
        var entries = JsonConvert.DeserializeObject<List<object>>(content);

        if (entries?.Count > 0)
        {
            var result = entries.Select(obj => JsonConvert.SerializeObject(obj)).ToArray();

            // Fix all strings
            for (int i = 0; i < result.Length; i++)
            {
                FixString(ref result[i]);
            }

            // Populate UI.
            txtQuestion.Text = result[0];

            // Lets not object pool this for now.
            pnlAwnsers.Children.Clear();

            for (int i = 1; i < result.Length; i++)
            {
                Button btn = new Button();
                btn.Width = 200;
                btn.Height = 40;
                btn.HorizontalAlignment = HorizontalAlignment.Center;
                btn.Content = result[i];

                string anwer = result[i];
                btn.Click += (one, two) => { btnGetResult(anwer); };

                pnlAwnsers.Children.Add(btn);
            }
        }
    }

    private async void btnGetResult(string anwer)
    {
        pnlAwnsers.Children.Clear();

        string url = $"http://localhost:8080/guess?answer={anwer}";
        HttpResponseMessage response = await client.GetAsync(url);

        var content = await response.Content.ReadAsStringAsync();

        // Display result box.
        txtResult.Visibility = Visibility.Visible;
        txtResult.Text = content == "true" ? "Correct!" : "Wrong!";

    }

    private void FixString(ref string target)
    {
        string search = '"'.ToString();
        string replace = "";

        target = target.Replace(search, replace);

        search = "&quot;";
        replace = '"'.ToString();

        target = target.Replace(search, replace);

        search = "&#039;";
        replace = "'";

        target = target.Replace(search, replace);
    }
}