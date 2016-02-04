var TemplateFile = React.createClass({
    render: function () {
        return (
            <tr className="templateFile">
                <td>{this.props.name}</td>
                <td>{this.props.sizeInBytes} Bytes</td>
            </tr>
        );
    }
});

var TemplateFileList = React.createClass({
    getInitialState: function () {
        return {data: []};
    },
    componentDidMount: function () {
        $.ajax({
            url: this.props.url,
            dataType: 'json',
            cache: false,
            success: function (data) {
                this.setState({data: data});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    render: function () {
        var templates = this.state.data.map(function (template) {
            return <TemplateFile name={template.name} sizeInBytes={template.sizeInBytes}/>;
        });
        return (
            <div className="templateFileList">
                <table className="pure-table pure-table-horizontal">
                    <thead>
                    <tr>
                        <td>Name</td>
                        <td>Dateigröße</td>
                    </tr>
                    </thead>
                    <tbody>
                    {templates}
                    </tbody>
                </table>
            </div>
        );
    }
});

ReactDOM.render(
    <TemplateFileList url="/api/template-views"/>,
    document.getElementById("templateFileList")
);
