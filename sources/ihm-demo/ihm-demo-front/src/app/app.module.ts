import { BrowserModule} from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {CookieService} from 'angular2-cookie/core';

import { ButtonModule, CalendarModule, MenubarModule, BreadcrumbModule, DropdownModule,
  ProgressBarModule, PaginatorModule, PanelModule, ListboxModule, GrowlModule, RadioButtonModule, TabViewModule,
  InputTextModule, DataTableModule, SharedModule, DialogModule, FieldsetModule, ToggleButtonModule,
  ConfirmDialogModule, ConfirmationService, OverlayPanelModule, InputSwitchModule, ChipsModule, MultiSelectModule,
  CheckboxModule} from 'primeng/primeng';

import { AppComponent } from './app.component';
import {MenuComponent} from './common/menu/menu.component';
import {BreadcrumbComponent} from './common/breadcrumb/breadcrumb.component';
import {ArchiveUnitHelper} from './archive-unit/archive-unit.helper';
import {ReferentialHelper} from './referentials/referential.helper';
import {ResourcesService} from './common/resources.service';
import {BreadcrumbService} from './common/breadcrumb.service';
import { IngestUtilsService } from './common/utils/ingest-utils.service';
import { LogbookService } from './ingest/logbook.service';
import { IngestService } from './ingest/ingest.service';
import { HomeComponent } from './home/home.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import { FileDropModule } from 'angular2-file-drop';

import { LogbookComponent } from './ingest/logbook/logbook.component';
import { ResultsComponent } from './common/results/results.component';
import { MetadataFieldComponent } from './common/metadata-field/metadata-field.component';
import { GenericTableComponent } from './common/generic-table/generic-table.component';
import { LogbookDetailsComponent } from './ingest/logbook/logbook-details/logbook-details.component';
import { UploadService } from './common/upload/upload.service';
import { SipComponent } from './ingest/sip/sip.component';
import { AuthenticationComponent } from './authentication/authentication.component';
import { AuthenticationService } from './authentication/authentication.service';
import { SearchComponent } from './common/search/search.component';
import { LogbookOperationComponent } from './admin/logbook-operation/logbook-operation.component';
import { LogbookOperationDetailsComponent } from './admin/logbook-operation/logbook-operation-details/logbook-operation-details.component';
import { LogbookOperationEventsComponent } from './common/logbook-operation-events/logbook-operation-events.component';
import { EventDisplayComponent } from './common/logbook-operation-events/event-display/event-display.component';
import { ArchiveUnitComponent } from './archive-unit/archive-unit-search/archive-unit.component';
import { ArchiveUnitDetailsComponent } from './archive-unit/archive-unit-details/archive-unit-details.component';
import { ArchiveMainDescriptionComponent } from './archive-unit/archive-unit-details/archive-main-description/archive-main-description.component';
import { ArchiveExtraDescriptionComponent } from './archive-unit/archive-unit-details/archive-extra-description/archive-extra-description.component';
import { ArchiveRuleBlocComponent } from './archive-unit/archive-unit-details/archive-rule-bloc/archive-rule-bloc.component';
import { ArchiveObjectGroupComponent } from './archive-unit/archive-unit-details/archive-object-group/archive-object-group.component';
import { ArchiveTreeViewComponent } from './archive-unit/archive-unit-details/archive-tree-view/archive-tree-view.component';
import { KeysPipe, BytesPipe } from './common/utils/pipes';
import { DateService } from './common/utils/date.service';
import { ObjectsService } from './common/utils/objects.service';
import {ArchiveUnitService} from "./archive-unit/archive-unit.service";
import { ImportComponent } from './referentials/import/import.component';
import { SearchReferentialsComponent } from './referentials/search-referentials/search-referentials.component';
import { ReferentialsService } from './referentials/referentials.service';
import { UploadReferentialsComponent } from './common/upload/upload-referentials/upload-referentials.component';
import { UploadSipComponent } from './common/upload/upload-sip/upload-sip.component';
import {AccessContractService} from "./common/access-contract.service";
import { FormatComponent } from './referentials/details/format/format.component';
import { RuleComponent } from './referentials/details/rule/rule.component';
import { AccessContractComponent } from './referentials/details/access-contract/access-contract.component';
import { IngestContractComponent } from './referentials/details/ingest-contract/ingest-contract.component';
import { ProfilComponent } from './referentials/details/profil/profil.component';
import { ContextComponent } from './referentials/details/context/context.component';
import { TreeParentComponent } from './archive-unit/archive-unit-details/archive-tree-view/tree-parent/tree-parent.component';
import { TreeChildComponent } from './archive-unit/archive-unit-details/archive-tree-view/tree-child/tree-child.component';
import { TreeSearchComponent } from './archive-unit/archive-unit-details/archive-tree-view/tree-search/tree-search.component';
import { AgenciesComponent } from './referentials/details/agencies/agencies.component';
import { AuditComponent } from './admin/audit/audit.component';
import { AuditService } from './admin/audit/audit.service';

const appRoutes: Routes = [
  {
    path: 'home', component: HomeComponent
  },
  {
    path: 'search/archiveUnit', component: ArchiveUnitComponent
  },
  {
    path: 'search/archiveUnit/:id', component: ArchiveUnitDetailsComponent
  },
  {
    path: 'login', component: AuthenticationComponent
  },
  {
    path: 'ingest/logbook', component: LogbookComponent
  },
  {
    path: 'ingest/logbook/:id', component: LogbookDetailsComponent
  },
  {
    path: 'ingest/sip', component: SipComponent
  },
  {
    path: 'admin/logbookOperation', component: LogbookOperationComponent
  },
  {
    path: 'admin/logbookOperation/:id', component: LogbookOperationDetailsComponent
  },
  {
    path: 'admin/import/:referentialType', component: ImportComponent
  },
  {
    path: 'admin/format/:id', component: FormatComponent
  },
  {
    path: 'admin/rule/:id', component: RuleComponent
  },
  {
    path: 'admin/accessContract/:id', component: AccessContractComponent
  },
  {
    path: 'admin/ingestContract/:id', component: IngestContractComponent
  },
  {
    path: 'admin/profil/:id', component: ProfilComponent
  },
  {
    path: 'admin/context/:id', component: ContextComponent
  },
  {
    path: 'admin/agencies/:id', component: AgenciesComponent
  },
  {
    path: 'admin/search/:referentialType', component: SearchReferentialsComponent
  },
  {
    path: 'admin/audits', component: AuditComponent
  },
  {
    path: '**', redirectTo: 'ingest/sip', pathMatch: 'full'
  }
];

@NgModule({
  declarations: [
    AppComponent,
    MenuComponent,
    BreadcrumbComponent,
    HomeComponent,
    LogbookComponent,
    ResultsComponent,
    GenericTableComponent,
    SipComponent,
    AuthenticationComponent,
    SearchComponent,
    LogbookDetailsComponent,
    LogbookOperationComponent,
    LogbookOperationDetailsComponent,
    LogbookOperationEventsComponent,
    EventDisplayComponent,
    ArchiveUnitComponent,
    ArchiveUnitDetailsComponent,
    ArchiveMainDescriptionComponent,
    ArchiveExtraDescriptionComponent,
    ArchiveRuleBlocComponent,
    ArchiveObjectGroupComponent,
    ArchiveTreeViewComponent,
    KeysPipe,
    BytesPipe,
    LogbookDetailsComponent,
    SipComponent,
    AuthenticationComponent,
    SearchComponent,
    ImportComponent,
    SearchReferentialsComponent,
    UploadReferentialsComponent,
    UploadSipComponent,
    FormatComponent,
    RuleComponent,
    AccessContractComponent,
    IngestContractComponent,
    ProfilComponent,
    ContextComponent,
    MetadataFieldComponent,
    TreeParentComponent,
    TreeChildComponent,
    TreeSearchComponent,
    AgenciesComponent,
    AuditComponent
  ],
  imports: [
    RouterModule.forRoot(appRoutes, {useHash: true}),
    BrowserModule,
    BrowserAnimationsModule,
    MenubarModule,
    ButtonModule,
    BreadcrumbModule,
    CalendarModule,
    DropdownModule,
    GrowlModule,
    PanelModule,
    RadioButtonModule,
    BrowserAnimationsModule,
    FormsModule,
    ListboxModule,
    PaginatorModule,
    FileDropModule,
    InputTextModule,
    TabViewModule,
    ToggleButtonModule,
    ProgressBarModule,
    DataTableModule,
    SharedModule,
    ReactiveFormsModule,
    CalendarModule,
    FieldsetModule,
    HttpModule,
    DialogModule,
    ConfirmDialogModule,
    InputSwitchModule,
    ChipsModule,
    OverlayPanelModule,
    MultiSelectModule,
    CheckboxModule
  ],
  providers: [
    ResourcesService,
    CookieService,
    BreadcrumbService,
    LogbookService,
    IngestService,
    IngestUtilsService,
    UploadService,
    AuthenticationService,
    ArchiveUnitHelper,
    ReferentialHelper,
    ArchiveUnitService,
    ReferentialsService,
    DateService,
    AccessContractService,
    ConfirmationService,
    ObjectsService,
    AuditService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
